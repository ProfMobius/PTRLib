package com.mia.craftstudio.minecraft;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import com.mia.craftstudio.libgdx.Matrix4;
import com.mia.craftstudio.libgdx.Vector3;

public class ProjectionHelper {

	public static Matrix4 getOrthoMatrix(){
		//GL Definition of the command
		//GL11.glOrtho(left, right, bottom, top, zNear, zFar);
		//[ 2 / (right - left)	0					0				tx]
		//[	0					2/(top - bottom)	0				ty]
		//[	0					0				   -2/(far - near)	tz]
		//[ 0					0					0				1 ]
		//tx = - (right + left)  / (right - left)
		//ty = - (top   + bottom)/ (top   - bottom)
		//tz = - (far   + near)  / (far   - near)

		//A few way Minecraft setup the orthographic view for the inventory
		//GL11.glOrtho(0.0D, (double)scaledresolution.getScaledWidth(), (double)scaledresolution.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
		//GL11.glOrtho(0.0D, (double)this.displayWidth, (double)this.displayHeight, 0.0D, 1000.0D, 3000.0D);
		//GL11.glOrtho(0.0D, scaledresolution.getScaledWidth_double(), scaledresolution.getScaledHeight_double(), 0.0D, 1000.0D, 3000.0D);

		// 2 projection matrices are available relative to the inventory. Which one is the good one ?
		// 2nd one is formatted like an ortho matrix, but is dependent on the actual size of the screen ?
		//======
		//0.8027   0.0000   0.0000   0.0000
		//0.0000   1.4281   0.0000   0.0000
		//0.0000   0.0000  -1.0003  -0.1000
		//0.0000   0.0000  -1.0000   0.0000
		//======
		//0.0047   0.0000   0.0000  -1.0000
		//0.0000  -0.0083   0.0000   1.0000
		//0.0000   0.0000  -0.0010  -2.0000
		//0.0000   0.0000   0.0000   1.0000

		// After full screen
		//======
		//0.7639   0.0000   0.0000   0.0000
		//0.0000   1.4281   0.0000   0.0000
		//0.0000   0.0000  -1.0003  -0.1000
		//0.0000   0.0000  -1.0000   0.0000
		//======
		//0.0031   0.0000   0.0000  -1.0000
		//0.0000  -0.0058   0.0000   1.0000
		//0.0000   0.0000  -0.0010  -2.0000
		//0.0000   0.0000   0.0000   1.0000

		//======
		//0.8027060032     0.0000000000     0.0000000000     0.0000000000
		//0.0000000000     1.4281477928     0.0000000000     0.0000000000
		//0.0000000000     0.0000000000    -1.0002603531    -0.1000130251
		//0.0000000000     0.0000000000    -1.0000000000     0.0000000000
		//======
		//0.0046838406     0.0000000000     0.0000000000    -1.0000000000
		//0.0000000000    -0.0083333338     0.0000000000     1.0000000000
		//0.0000000000     0.0000000000    -0.0010000000    -2.0000000000
		//0.0000000000     0.0000000000     0.0000000000     1.0000000000

		final Minecraft mc = Minecraft.getMinecraft();
		final ScaledResolution rez = new ScaledResolution(mc);

		final float left   = 0.0F;
		final float right  = rez.getScaledWidth();
		final float bottom = rez.getScaledHeight();
		final float top    = 0.0F;
		final float near   = 1000.0F;
		final float far    = 3000.0F;

		final float tx     = - (right + left)  /(right - left);
		final float ty     = - (top   + bottom)/(top   - bottom);
		final float tz     = - (far   + near)  /(far   - near);

		final Matrix4 ortho = new Matrix4();
		ortho.val[Matrix4.M00] =  2f/(right - left);
		ortho.val[Matrix4.M11] =  2f/(top   - bottom);
		ortho.val[Matrix4.M22] = -2f/(far   - near);
		ortho.val[Matrix4.M33] =  1f;
		ortho.val[Matrix4.M03] =  tx;
		ortho.val[Matrix4.M13] =  ty;
		ortho.val[Matrix4.M23] =  tz;

		return ortho;
	}

	public static Vector3[] getExtend(final Vector3[] vertices){
		// We compute the min/max of the vertices to pregenerate potential AABB
		float xMin = Float.MAX_VALUE;
		float yMin = Float.MAX_VALUE;
		float zMin = Float.MAX_VALUE;
		float xMax = -Float.MAX_VALUE; // We can't use Float.MIN_VALUE because that actually defines the smallest acceptable
		float yMax = -Float.MAX_VALUE; // PRECISION that is detectable as a difference in float values.  Since JAVA uses the
		float zMax = -Float.MAX_VALUE; // uppermost bit as a sign bit, the REAL_MIN_VALUE = -MAX_VALUE

		for (int i = 0; i < 8; i++){
			xMin = Math.min(xMin, vertices[i].x);
			yMin = Math.min(yMin, vertices[i].y);
			zMin = Math.min(zMin, vertices[i].z);

			xMax = Math.max(xMax, vertices[i].x);
			yMax = Math.max(yMax, vertices[i].y);
			zMax = Math.max(zMax, vertices[i].z);
		}

		final Vector3[] extend = new Vector3[2];

		extend[0] = new Vector3(xMin, yMin, zMin);
		extend[1] = new Vector3(xMax, yMax, zMax);

		return extend;
	}

	public static void printMatrix(final int glMatrix){
		final FloatBuffer projmat = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(glMatrix, projmat);
		for (int i = 0; i < 4; i++){
			System.out.printf("%16.10f " ,projmat.get(i));
			System.out.printf("%16.10f " ,projmat.get(i+4));
			System.out.printf("%16.10f " ,projmat.get(i+8));
			System.out.printf("%16.10f " ,projmat.get(i+12));
			System.out.printf("\n");
		}
		System.out.printf("======\n");
	}

	public static Matrix4 getMatrix(final int glMatrix){
		final FloatBuffer projmat = BufferUtils.createFloatBuffer(16);
		GL11.glGetFloat(glMatrix, projmat);
		final Matrix4 retmat = new Matrix4();
		for (int i = 0; i < 16; i++) {
			retmat.val[i] = projmat.get();
		}

		return retmat;
	}
}
