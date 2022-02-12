package com.sarim.sceneformgesturestutorial

import android.animation.Animator
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentOnAttachListener
import com.google.ar.core.*
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.ArFragment.OnViewCreatedListener
import com.google.ar.sceneform.ux.BaseArFragment.OnSessionConfigurationListener
import com.google.ar.sceneform.ux.BaseArFragment.OnTapArPlaneListener
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*


class MainActivity : AppCompatActivity(), FragmentOnAttachListener,
    OnTapArPlaneListener, OnSessionConfigurationListener, OnViewCreatedListener {
    private var arFragment: ArFragment? = null
    private var model: Renderable? = null
    private var modelAnimator: ObjectAnimator? = null
    private val modelSound = MediaPlayer()
    private var viewRenderable: ViewRenderable? = null
    private var gestureDetectorCompat: GestureDetectorCompat? = null
    private var gestureDetector: GestureDetector = GestureDetector()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.addFragmentOnAttachListener(this)
        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                supportFragmentManager.beginTransaction()
                    .add(R.id.arFragment, ArFragment::class.java, null)
                    .commit()
            }
        }
        loadModels()
        gestureDetectorCompat = GestureDetectorCompat(this, gestureDetector)
    }

    override fun onAttachFragment(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment.id == R.id.arFragment) {
            arFragment = fragment as ArFragment
            arFragment!!.setOnTapArPlaneListener(this)
            arFragment!!.setOnViewCreatedListener(this)
            arFragment!!.setOnSessionConfigurationListener(this)
        }
    }

    override fun onViewCreated(arSceneView: ArSceneView) {
        arFragment!!.setOnViewCreatedListener(null)

        arSceneView.scene.setOnTouchListener {
            hitTestResult : HitTestResult?, motionEvent : MotionEvent? ->
            gestureDetectorCompat!!.onTouchEvent(motionEvent)
            when (gestureDetector.gestureType) {
                GestureDetector.GestureType.SINGLE_TAP -> {
                    if (hitTestResult!!.node == null) {
                        Toast.makeText(this, "SINGLE TAP performed", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(this, "SINGLE TAP performed on a node", Toast.LENGTH_SHORT).show()
                    }
                }
                GestureDetector.GestureType.DOUBLE_TAP -> {
                    if (hitTestResult!!.node == null) {
                        Toast.makeText(this, "DOUBLE TAP performed", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        val anchorNode: AnchorNode = hitTestResult.node?.parent as AnchorNode
                        anchorNode.anchor?.detach()
                        hitTestResult.node?.parent = null
                        Toast.makeText(this, "DOUBLE TAP performed on a node", Toast.LENGTH_SHORT).show()
                    }
                }
                GestureDetector.GestureType.LONG_PRESS -> {
                    if (hitTestResult!!.node == null) {
                        Toast.makeText(this, "LONG PRESS performed", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        hitTestResult.node?.worldScale = Vector3(0.4f, 0.4f, 0.4f)
                        Toast.makeText(this, "LONG PRESS performed on a node", Toast.LENGTH_SHORT).show()
                    }
                }
                else -> { }
            }
            gestureDetector.resetGestureType()
            true
        }
    }

    override fun onStart() {
        super.onStart()
        if (modelAnimator != null && modelAnimator!!.isPaused) {
            modelAnimator!!.start()
            modelSound.start()
        }
    }

    override fun onStop() {
        super.onStop()
        if (modelAnimator != null && modelAnimator!!.isRunning) {
            modelAnimator!!.pause()
            modelSound.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
        modelSound.release()
    }

    fun loadModels() {
        val weakActivity = WeakReference(this)
        ModelRenderable.builder()
            .setSource(
                this,
                Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/GiantPanda/model.glb")
            )
            .setIsFilamentGltf(true)
            .setAsyncLoadEnabled(true)
            .build()
            .thenAccept { model: ModelRenderable? ->
                val activity = weakActivity.get()
                if (activity != null) {
                    activity.model = model
                }
            }
            .exceptionally { throwable: Throwable? ->
                Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show()
                null
            }
        ViewRenderable.builder()
            .setView(this, R.layout.view_model_title)
            .build()
            .thenAccept { viewRenderable: ViewRenderable? ->
                val activity = weakActivity.get()
                if (activity != null) {
                    activity.viewRenderable = viewRenderable
                }
            }
            .exceptionally { throwable: Throwable? ->
                Toast.makeText(this, "Unable to load model", Toast.LENGTH_LONG).show()
                null
            }
    }

    var createdModel = false

    override fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (model == null || viewRenderable == null) {
            Toast.makeText(this, "Loading...", Toast.LENGTH_SHORT).show()
            return
        }
        if (createdModel) {
            return
        }

        // Create the Anchor.
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.parent = arFragment!!.arSceneView.scene

        // Create the transformable model and add it to the anchor.
        val modelNode = Node()
        modelNode.parent = anchorNode
        val modelInstance = modelNode.setRenderable(model)
        modelAnimator = modelInstance.animate(true)
        modelAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                playSound()
            }

            override fun onAnimationEnd(animation: Animator) {
                stopSound()
            }

            override fun onAnimationCancel(animation: Animator) {
                stopSound()
            }

            override fun onAnimationRepeat(animation: Animator) {
                stopSound()
                playSound()
            }
        })
        modelAnimator?.start()
        modelNode.worldScale = Vector3(0.1f, 0.1f, 0.1f)
        val titleNode = Node()
        titleNode.parent = modelNode
        titleNode.isEnabled = false
        titleNode.localPosition = Vector3(0.0f, 1.0f, 0.0f)
        titleNode.worldScale = Vector3(0.1f, 0.1f, 0.1f)
        titleNode.renderable = viewRenderable
        titleNode.isEnabled = true
        createdModel = true
    }

    private fun playSound() {
        try {
            // Can't figure out why does the repeat function doesn't for remote url
            modelSound.setDataSource("https://storage.googleapis.com/ar-answers-in-search-models/static/GiantPanda/Bear_Panda_Giant_Unisex_Adult.ogg")
            modelSound.prepare()
            modelSound.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopSound() {
        modelSound.stop()
        modelSound.reset()
    }

    override fun onSessionConfiguration(session: Session, config: Config) {
        config.depthMode = Config.DepthMode.DISABLED
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        config.cloudAnchorMode = Config.CloudAnchorMode.DISABLED
        config.augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
        config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
        config.lightEstimationMode = Config.LightEstimationMode.DISABLED
        config.focusMode = Config.FocusMode.AUTO

        val cameraConfigFilter = CameraConfigFilter(session)
            .setTargetFps(
                EnumSet.of(
                    CameraConfig.TargetFps.TARGET_FPS_30,
                    CameraConfig.TargetFps.TARGET_FPS_60
                )
            )
        val cameraConfigsList = session.getSupportedCameraConfigs(cameraConfigFilter)
        for (currentCameraConfig in cameraConfigsList) {
            if (currentCameraConfig.imageSize.toString() == "640x480") {
                session.cameraConfig = currentCameraConfig
                break
            }
        }
        session.configure(config)
    }
}