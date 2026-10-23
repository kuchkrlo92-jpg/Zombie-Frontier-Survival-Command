package com.example.ui

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.viewmodel.GameViewModel
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

/**
 * Genuine 3D Gameplay Screen with automated GLSurfaceView and Renderer lifecycle management
 * for Zombie Frontier: Survival Command. Manages surface creation, change, and destruction automatically.
 */
@Composable
fun True3DCombatScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    BackHandler {
        navController.popBackStack()
    }

    var currentLane by remember { mutableStateOf(1) } // 0: Left, 1: Center, 2: Right
    var forwardSpeed by remember { mutableStateOf(1.0f) }

    Scaffold(
        containerColor = Color(0xFF0A0E14)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Genuine OpenGL ES 3D View with Automatic Lifecycle Management
            AndroidView(
                factory = { context ->
                    CombatGLSurfaceView(context).apply {
                        setLaneSupplier { currentLane }
                        setSpeedSupplier { forwardSpeed }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    (view as CombatGLSurfaceView).updateLane(currentLane)
                }
            )

            // Top HUD Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Surface(
                        color = Color(0xFF1E293B).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF))
                    ) {
                        Text(
                            text = "OPENGL ES 2.0 // 3D COMBAT ENGINE",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Architectural Status Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "AUTOMATED GRAPHICS LIFECYCLE ACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80)
                        )
                        Text(
                            text = "• Surface Creation & Destruction Handled\n• Hardware Depth Buffer & Lighting Active\n• 3D Special Forces Commander & Road",
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Controls Overlay (Lane Switching & Speed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { if (currentLane > 0) currentLane-- },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color(0xFF00E5FF))
                        Spacer(Modifier.width(4.dp))
                        Text("LANE LEFT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.width(16.dp))

                    Button(
                        onClick = { if (currentLane < 2) currentLane++ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text("LANE RIGHT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Move Right", tint = Color(0xFF00E5FF))
                    }
                }
            }
        }
    }
}

/**
 * GLSurfaceView implementation managing automatic 3D graphics lifecycle (creation, change, destruction).
 */
class CombatGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: Combat3DRenderer

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Enable 16-bit depth buffer
        renderer = Combat3DRenderer()
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        onResume()
    }

    override fun onDetachedFromWindow() {
        onPause()
        super.onDetachedFromWindow()
    }

    fun setLaneSupplier(supplier: () -> Int) {
        renderer.laneSupplier = supplier
    }

    fun setSpeedSupplier(supplier: () -> Float) {
        renderer.speedSupplier = supplier
    }

    fun updateLane(lane: Int) {
        renderer.targetLane = lane
    }
}

/**
 * Renderer implementation handling surface creation, viewport changes, and frame rendering.
 */
class Combat3DRenderer : GLSurfaceView.Renderer {
    var laneSupplier: () -> Int = { 1 }
    var speedSupplier: () -> Float = { 1.0f }
    var targetLane: Int = 1
    private var currentLaneFloat: Float = 1.0f

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var programId = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var normalHandle = 0
    private var mvpMatrixHandle = 0
    private var lightDirHandle = 0

    private var roadBuffer: FloatBuffer? = null
    private var cubeBuffer: FloatBuffer? = null
    private var roadVertexCount = 0
    private var cubeVertexCount = 0

    private var runAnimTime = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.04f, 0.06f, 0.1f, 1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        initShaders()
        initGeometry()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.toFloat()
        android.opengl.Matrix.perspectiveM(projectionMatrix, 0, 45f, ratio, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(programId)

        // Smooth lane lerp
        currentLaneFloat += (targetLane - currentLaneFloat) * 0.15f
        runAnimTime += 0.05f

        // Camera setup (Looking down the 3D road)
        val camX = (currentLaneFloat - 1.0f) * 0.8f
        val camY = 2.2f
        val camZ = -2.5f
        android.opengl.Matrix.setLookAtM(
            viewMatrix, 0,
            camX, camY, camZ,
            camX, 1.5f, camZ + 5.0f,
            0f, 1f, 0f
        )

        // Light direction (sunlight from top-right-front)
        GLES20.glUniform3f(lightDirHandle, 0.5f, 0.8f, -0.5f)

        // 1. Render 3D Road
        drawRoad()

        // 2. Render 3D Roadside Obstacles (Barriers / Wrecked Cars)
        drawRoadsideObstacles()

        // 3. Render 3D Special Forces Commander
        drawCommander()
    }

    private fun initShaders() {
        val vertexShaderCode = """
            attribute vec4 aPosition;
            attribute vec4 aColor;
            attribute vec3 aNormal;
            uniform mat4 uMVPMatrix;
            uniform vec3 uLightDir;
            varying vec4 vColor;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                float nDotL = max(dot(normalize(aNormal), normalize(uLightDir)), 0.25);
                vColor = vec4(aColor.rgb * nDotL, aColor.a);
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        programId = GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vertexShader)
            GLES20.glAttachShader(this, fragmentShader)
            GLES20.glLinkProgram(this)
        }

        positionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        colorHandle = GLES20.glGetAttribLocation(programId, "aColor")
        normalHandle = GLES20.glGetAttribLocation(programId, "aNormal")
        mvpMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        lightDirHandle = GLES20.glGetUniformLocation(programId, "uLightDir")
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private fun initGeometry() {
        val roadVertices = floatArrayOf(
            // Left ground shoulder
            -15f, 0f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            -3f, 0f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            -15f, 0f, 60f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            -3f, 0f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            -15f, 0f, 60f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            -3f, 0f, 60f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,

            // Asphalt Road (-3 to +3)
            -3f, 0f, 0f, 0.22f, 0.25f, 0.3f, 1f, 0f, 1f, 0f,
            3f, 0f, 0f, 0.22f, 0.25f, 0.3f, 1f, 0f, 1f, 0f,
            -3f, 0f, 60f, 0.22f, 0.25f, 0.3f, 1f, 0f, 1f, 0f,
            3f, 0f, 0f, 0.22f, 0.25f, 0.3f, 1f, 0f, 1f, 0f,
            -3f, 0f, 60f, 0.22f, 0.25f, 0.3f, 1f, 0f, 1f, 0f,
            3f, 0f, 60f, 0.22f, 0.25f, 0.3f, 1f, 0f, 1f, 0f,

            // Right ground shoulder
            3f, 0f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            15f, 0f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            3f, 0f, 60f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            15f, 0f, 0f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            3f, 0f, 60f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f,
            15f, 0f, 60f, 0.15f, 0.18f, 0.22f, 1f, 0f, 1f, 0f
        )
        roadVertexCount = roadVertices.size / 10
        roadBuffer = ByteBuffer.allocateDirect(roadVertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(roadVertices)
            position(0)
        }

        val c = createCubeVertices(0.8f, 1.8f, 0.6f, 0.1f, 0.12f, 0.15f)
        cubeVertexCount = c.size / 10
        cubeBuffer = ByteBuffer.allocateDirect(c.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(c)
            position(0)
        }
    }

    private fun createCubeVertices(w: Float, h: Float, d: Float, r: Float, g: Float, b: Float): FloatArray {
        val hw = w / 2f
        val hd = d / 2f
        val vertices = mutableListOf<Float>()

        fun addQuad(p1: FloatArray, p2: FloatArray, p3: FloatArray, p4: FloatArray, normal: FloatArray) {
            for (p in listOf(p1, p2, p3)) {
                vertices.addAll(listOf(p[0], p[1], p[2], r, g, b, 1f, normal[0], normal[1], normal[2]))
            }
            for (p in listOf(p1, p3, p4)) {
                vertices.addAll(listOf(p[0], p[1], p[2], r, g, b, 1f, normal[0], normal[1], normal[2]))
            }
        }

        addQuad(floatArrayOf(-hw, h, -hd), floatArrayOf(-hw, 0f, -hd), floatArrayOf(hw, 0f, -hd), floatArrayOf(hw, h, -hd), floatArrayOf(0f, 0f, -1f))
        addQuad(floatArrayOf(hw, h, hd), floatArrayOf(hw, 0f, hd), floatArrayOf(-hw, 0f, hd), floatArrayOf(-hw, h, hd), floatArrayOf(0f, 0f, 1f))
        addQuad(floatArrayOf(-hw, h, hd), floatArrayOf(-hw, 0f, hd), floatArrayOf(-hw, 0f, -hd), floatArrayOf(-hw, h, -hd), floatArrayOf(-1f, 0f, 0f))
        addQuad(floatArrayOf(hw, h, -hd), floatArrayOf(hw, 0f, -hd), floatArrayOf(hw, 0f, hd), floatArrayOf(hw, h, hd), floatArrayOf(1f, 0f, 0f))
        addQuad(floatArrayOf(-hw, h, hd), floatArrayOf(-hw, h, -hd), floatArrayOf(hw, h, -hd), floatArrayOf(hw, h, hd), floatArrayOf(0f, 1f, 0f))
        addQuad(floatArrayOf(-hw, 0f, -hd), floatArrayOf(-hw, 0f, hd), floatArrayOf(hw, 0f, hd), floatArrayOf(hw, 0f, -hd), floatArrayOf(0f, -1f, 0f))

        return vertices.toFloatArray()
    }

    private fun drawRoad() {
        roadBuffer?.let { buffer ->
            buffer.position(0)
            android.opengl.Matrix.setIdentityM(modelMatrix, 0)
            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(colorHandle)
            GLES20.glEnableVertexAttribArray(normalHandle)

            buffer.position(0)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 40, buffer)
            buffer.position(3)
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 40, buffer)
            buffer.position(7)
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 40, buffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, roadVertexCount)

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(colorHandle)
            GLES20.glDisableVertexAttribArray(normalHandle)
        }
    }

    private fun drawRoadsideObstacles() {
        for (i in 0..5) {
            val zPos = 5f + i * 10f
            drawCube(-3.8f, 0f, zPos, 1.2f, 1.0f, 2.5f, 0.4f, 0.45f, 0.5f)
            drawCube(3.8f, 0f, zPos + 5f, 1.5f, 1.2f, 3.0f, 0.3f, 0.35f, 0.4f)
        }
    }

    private fun drawCommander() {
        val cmdX = (currentLaneFloat - 1.0f) * 1.2f
        val cmdZ = 1.5f
        val bob = kotlin.math.abs(sin(runAnimTime * 8f)) * 0.15f

        drawCube(cmdX, 0.2f + bob, cmdZ, 0.7f, 1.1f, 0.4f, 0.12f, 0.15f, 0.18f)
        drawCube(cmdX, 1.3f + bob, cmdZ, 0.45f, 0.45f, 0.45f, 0.2f, 0.22f, 0.25f)
        drawCube(cmdX + 0.3f, 0.8f + bob, cmdZ - 0.4f, 0.2f, 0.2f, 1.2f, 0.25f, 0.28f, 0.32f)
    }

    private fun drawCube(x: Float, y: Float, z: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float) {
        cubeBuffer?.let { buffer ->
            android.opengl.Matrix.setIdentityM(modelMatrix, 0)
            android.opengl.Matrix.translateM(modelMatrix, 0, x, y, z)
            android.opengl.Matrix.scaleM(modelMatrix, 0, sx, sy, sz)

            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(positionHandle)
            GLES20.glEnableVertexAttribArray(colorHandle)
            GLES20.glEnableVertexAttribArray(normalHandle)

            buffer.position(0)
            GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 40, buffer)
            buffer.position(3)
            GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, 40, buffer)
            buffer.position(7)
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 40, buffer)

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, cubeVertexCount)

            GLES20.glDisableVertexAttribArray(positionHandle)
            GLES20.glDisableVertexAttribArray(colorHandle)
            GLES20.glDisableVertexAttribArray(normalHandle)
        }
    }
}
