package com.surface

import android.opengl.GLSurfaceView
import android.opengl.GLU
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyOpenGLRender: GLSurfaceView.Renderer {

    private val mCube = Cube()
    private var mCubeRotation: Float = 0.0F

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        gl?.apply {
            glClearColor(0.0f, 0.0f, 0.0f, 0.5f)
            glClearDepthf(1.0f)
            glEnable(GL10.GL_DEPTH_TEST)
            glDepthFunc(GL10.GL_LEQUAL)
            glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        gl?.glViewport(0, 0, width, height)
        gl?.glMatrixMode(GL10.GL_PROJECTION)
        gl?.glLoadIdentity()
        GLU.gluPerspective(gl, 45.0f, width.toFloat()/height, 0.1f, 100.0f);
        gl?.glViewport(0, 0, width, height)

        gl?.glMatrixMode(GL10.GL_MODELVIEW)
        gl?.glLoadIdentity()
    }

    override fun onDrawFrame(gl: GL10?) {
        gl?.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        gl?.glLoadIdentity()
        gl?.glTranslatef(0.0f, 0.0f, -10.0f)
        gl?.glRotatef(mCubeRotation, 1.0f, 1.0f, 1.0f)
        mCube.draw(gl)
        gl?.glLoadIdentity()
        mCubeRotation -= 0.15f;
    }
}