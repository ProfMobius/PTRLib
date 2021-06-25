/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mia.craftstudio.libgdx;

import java.io.Serializable;

/** A specialized 3x3 matrix that can represent sequences of 2D translations, scales, flips, rotations, and shears. <a
 * href="http://en.wikipedia.org/wiki/Affine_transformation">Affine transformations</a> preserve straight lines, and
 * parallel lines remain parallel after the transformation. Operations on affine matrices are faster because the last row can
 * always be assumed (0, 0, 1).
 *
 * @author vmilea */
public final class Affine2 implements Serializable {
	private static final long serialVersionUID = 1524569123485049187L;

	public float m00 = 1, m01 = 0, m02 = 0;
	public float m10 = 0, m11 = 1, m12 = 0;

	// constant: m21 = 0, m21 = 1, m22 = 1

	/** Constructs an identity matrix. */
	public Affine2 () {
	}

	/** Constructs a matrix from the given affine matrix.
	 *
	 * @param other The affine matrix to copy. This matrix will not be modified. */
	public Affine2 (final Affine2 other) {
		set(other);
	}

	/** Sets this matrix to the identity matrix
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 idt () {
		m00 = 1;
		m01 = 0;
		m02 = 0;
		m10 = 0;
		m11 = 1;
		m12 = 0;
		return this;
	}

	/** Copies the values from the provided affine matrix to this matrix.
	 * @param other The affine matrix to copy.
	 * @return This matrix for the purposes of chaining. */
	public Affine2 set (final Affine2 other) {
		m00 = other.m00;
		m01 = other.m01;
		m02 = other.m02;
		m10 = other.m10;
		m11 = other.m11;
		m12 = other.m12;
		return this;
	}

	/** Copies the values from the provided matrix to this matrix.
	 * @param matrix The matrix to copy, assumed to be an affine transformation.
	 * @return This matrix for the purposes of chaining. */
	public Affine2 set (final Matrix3 matrix) {
		final float[] other = matrix.val;

		m00 = other[Matrix3.M00];
		m01 = other[Matrix3.M01];
		m02 = other[Matrix3.M02];
		m10 = other[Matrix3.M10];
		m11 = other[Matrix3.M11];
		m12 = other[Matrix3.M12];
		return this;
	}

	/** Copies the 2D transformation components from the provided 4x4 matrix. The values are mapped as follows:
	 *
	 * <pre>
	 *      [  M00  M01  M03  ]
	 *      [  M10  M11  M13  ]
	 *      [   0    0    1   ]
	 * </pre>
	 * @param matrix The source matrix, assumed to be an affine transformation within XY plane. This matrix will not be modified.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 set (final Matrix4 matrix) {
		final float[] other = matrix.val;

		m00 = other[Matrix4.M00];
		m01 = other[Matrix4.M01];
		m02 = other[Matrix4.M03];
		m10 = other[Matrix4.M10];
		m11 = other[Matrix4.M11];
		m12 = other[Matrix4.M13];
		return this;
	}

	/** Sets this matrix to a translation matrix.
	 * @param x The translation in x
	 * @param y The translation in y
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTranslation (final float x, final float y) {
		m00 = 1;
		m01 = 0;
		m02 = x;
		m10 = 0;
		m11 = 1;
		m12 = y;
		return this;
	}

	/** Sets this matrix to a translation matrix.
	 * @param trn The translation vector.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTranslation (final Vector2 trn) {
		return setToTranslation(trn.x, trn.y);
	}

	/** Sets this matrix to a scaling matrix.
	 * @param scaleX The scale in x.
	 * @param scaleY The scale in y.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToScaling (final float scaleX, final float scaleY) {
		m00 = scaleX;
		m01 = 0;
		m02 = 0;
		m10 = 0;
		m11 = scaleY;
		m12 = 0;
		return this;
	}

	/** Sets this matrix to a scaling matrix.
	 * @param scale The scale vector.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToScaling (final Vector2 scale) {
		return setToScaling(scale.x, scale.y);
	}

	/** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
	 * @param degrees The angle in degrees.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToRotation (final float degrees) {
		final float cos = MathUtils.cosDeg(degrees);
		final float sin = MathUtils.sinDeg(degrees);

		m00 = cos;
		m01 = -sin;
		m02 = 0;
		m10 = sin;
		m11 = cos;
		m12 = 0;
		return this;
	}

	/** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
	 * @param radians The angle in radians.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToRotationRad (final float radians) {
		final float cos = MathUtils.cos(radians);
		final float sin = MathUtils.sin(radians);

		m00 = cos;
		m01 = -sin;
		m02 = 0;
		m10 = sin;
		m11 = cos;
		m12 = 0;
		return this;
	}

	/** Sets this matrix to a rotation matrix that will rotate any vector in counter-clockwise direction around the z-axis.
	 * @param cos The angle cosine.
	 * @param sin The angle sine.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToRotation (final float cos, final float sin) {
		m00 = cos;
		m01 = -sin;
		m02 = 0;
		m10 = sin;
		m11 = cos;
		m12 = 0;
		return this;
	}

	/** Sets this matrix to a shearing matrix.
	 * @param shearX The shear in x direction.
	 * @param shearY The shear in y direction.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToShearing (final float shearX, final float shearY) {
		m00 = 1;
		m01 = shearX;
		m02 = 0;
		m10 = shearY;
		m11 = 1;
		m12 = 0;
		return this;
	}

	/** Sets this matrix to a shearing matrix.
	 * @param shear The shear vector.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToShearing (final Vector2 shear) {
		return setToShearing(shear.x, shear.y);
	}

	/** Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
	 * <code>idt().translate(x, y).rotate(degrees).scale(scaleX, scaleY)</code>
	 * @param x The translation in x.
	 * @param y The translation in y.
	 * @param degrees The angle in degrees.
	 * @param scaleX The scale in y.
	 * @param scaleY The scale in x.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTrnRotScl (final float x, final float y, final float degrees, final float scaleX, final float scaleY) {
		m02 = x;
		m12 = y;

		if (degrees == 0) {
			m00 = scaleX;
			m01 = 0;
			m10 = 0;
			m11 = scaleY;
		} else {
			final float sin = MathUtils.sinDeg(degrees);
			final float cos = MathUtils.cosDeg(degrees);

			m00 = cos * scaleX;
			m01 = -sin * scaleY;
			m10 = sin * scaleX;
			m11 = cos * scaleY;
		}
		return this;
	}

	/** Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
	 * <code>idt().translate(trn).rotate(degrees).scale(scale)</code>
	 * @param trn The translation vector.
	 * @param degrees The angle in degrees.
	 * @param scale The scale vector.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTrnRotScl (final Vector2 trn, final float degrees, final Vector2 scale) {
		return setToTrnRotScl(trn.x, trn.y, degrees, scale.x, scale.y);
	}

	/** Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
	 * <code>idt().translate(x, y).rotateRad(radians).scale(scaleX, scaleY)</code>
	 * @param x The translation in x.
	 * @param y The translation in y.
	 * @param radians The angle in radians.
	 * @param scaleX The scale in y.
	 * @param scaleY The scale in x.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTrnRotRadScl (final float x, final float y, final float radians, final float scaleX, final float scaleY) {
		m02 = x;
		m12 = y;

		if (radians == 0) {
			m00 = scaleX;
			m01 = 0;
			m10 = 0;
			m11 = scaleY;
		} else {
			final float sin = MathUtils.sin(radians);
			final float cos = MathUtils.cos(radians);

			m00 = cos * scaleX;
			m01 = -sin * scaleY;
			m10 = sin * scaleX;
			m11 = cos * scaleY;
		}
		return this;
	}

	/** Sets this matrix to a concatenation of translation, rotation and scale. It is a more efficient form for:
	 * <code>idt().translate(trn).rotateRad(radians).scale(scale)</code>
	 * @param trn The translation vector.
	 * @param radians The angle in radians.
	 * @param scale The scale vector.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTrnRotRadScl (final Vector2 trn, final float radians, final Vector2 scale) {
		return setToTrnRotRadScl(trn.x, trn.y, radians, scale.x, scale.y);
	}

	/** Sets this matrix to a concatenation of translation and scale. It is a more efficient form for:
	 * <code>idt().translate(x, y).scale(scaleX, scaleY)</code>
	 * @param x The translation in x.
	 * @param y The translation in y.
	 * @param scaleX The scale in y.
	 * @param scaleY The scale in x.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTrnScl (final float x, final float y, final float scaleX, final float scaleY) {
		m00 = scaleX;
		m01 = 0;
		m02 = x;
		m10 = 0;
		m11 = scaleY;
		m12 = y;
		return this;
	}

	/** Sets this matrix to a concatenation of translation and scale. It is a more efficient form for:
	 * <code>idt().translate(trn).scale(scale)</code>
	 * @param trn The translation vector.
	 * @param scale The scale vector.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToTrnScl (final Vector2 trn, final Vector2 scale) {
		return setToTrnScl(trn.x, trn.y, scale.x, scale.y);
	}

	/** Sets this matrix to the product of two matrices.
	 * @param l Left matrix.
	 * @param r Right matrix.
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 setToProduct (final Affine2 l, final Affine2 r) {
		m00 = l.m00 * r.m00 + l.m01 * r.m10;
		m01 = l.m00 * r.m01 + l.m01 * r.m11;
		m02 = l.m00 * r.m02 + l.m01 * r.m12 + l.m02;
		m10 = l.m10 * r.m00 + l.m11 * r.m10;
		m11 = l.m10 * r.m01 + l.m11 * r.m11;
		m12 = l.m10 * r.m02 + l.m11 * r.m12 + l.m12;
		return this;
	}

	/** Inverts this matrix given that the determinant is != 0.
	 * @return This matrix for the purpose of chaining operations.
	 * @throws GdxRuntimeException if the matrix is singular (not invertible) */
	public Affine2 inv () {
		final float det = det();
		//if (det == 0) throw new GdxRuntimeException("Can't invert a singular affine matrix");

		final float invDet = 1.0f / det;

		final float tmp00 = m11;
		final float tmp01 = -m01;
		final float tmp02 = m01 * m12 - m11 * m02;
		final float tmp10 = -m10;
		final float tmp11 = m00;
		final float tmp12 = m10 * m02 - m00 * m12;

		m00 = invDet * tmp00;
		m01 = invDet * tmp01;
		m02 = invDet * tmp02;
		m10 = invDet * tmp10;
		m11 = invDet * tmp11;
		m12 = invDet * tmp12;
		return this;
	}

	/** Postmultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
	 *
	 * <pre>
	 * A.mul(B) results in A := AB
	 * </pre>
	 * @param other Matrix to multiply by.
	 * @return This matrix for the purpose of chaining operations together. */
	public Affine2 mul (final Affine2 other) {
		final float tmp00 = m00 * other.m00 + m01 * other.m10;
		final float tmp01 = m00 * other.m01 + m01 * other.m11;
		final float tmp02 = m00 * other.m02 + m01 * other.m12 + m02;
		final float tmp10 = m10 * other.m00 + m11 * other.m10;
		final float tmp11 = m10 * other.m01 + m11 * other.m11;
		final float tmp12 = m10 * other.m02 + m11 * other.m12 + m12;

		m00 = tmp00;
		m01 = tmp01;
		m02 = tmp02;
		m10 = tmp10;
		m11 = tmp11;
		m12 = tmp12;
		return this;
	}

	/** Premultiplies this matrix with the provided matrix and stores the result in this matrix. For example:
	 *
	 * <pre>
	 * A.preMul(B) results in A := BA
	 * </pre>
	 * @param other The other Matrix to multiply by
	 * @return This matrix for the purpose of chaining operations. */
	public Affine2 preMul (final Affine2 other) {
		final float tmp00 = other.m00 * m00 + other.m01 * m10;
		final float tmp01 = other.m00 * m01 + other.m01 * m11;
		final float tmp02 = other.m00 * m02 + other.m01 * m12 + other.m02;
		final float tmp10 = other.m10 * m00 + other.m11 * m10;
		final float tmp11 = other.m10 * m01 + other.m11 * m11;
		final float tmp12 = other.m10 * m02 + other.m11 * m12 + other.m12;

		m00 = tmp00;
		m01 = tmp01;
		m02 = tmp02;
		m10 = tmp10;
		m11 = tmp11;
		m12 = tmp12;
		return this;
	}

	/** Postmultiplies this matrix by a translation matrix.
	 * @param x The x-component of the translation vector.
	 * @param y The y-component of the translation vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 translate (final float x, final float y) {
		m02 += m00 * x + m01 * y;
		m12 += m10 * x + m11 * y;
		return this;
	}

	/** Postmultiplies this matrix by a translation matrix.
	 * @param trn The translation vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 translate (final Vector2 trn) {
		return translate(trn.x, trn.y);
	}

	/** Premultiplies this matrix by a translation matrix.
	 * @param x The x-component of the translation vector.
	 * @param y The y-component of the translation vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preTranslate (final float x, final float y) {
		m02 += x;
		m12 += y;
		return this;
	}

	/** Premultiplies this matrix by a translation matrix.
	 * @param trn The translation vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preTranslate (final Vector2 trn) {
		return preTranslate(trn.x, trn.y);
	}

	/** Postmultiplies this matrix with a scale matrix.
	 * @param scaleX The scale in the x-axis.
	 * @param scaleY The scale in the y-axis.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 scale (final float scaleX, final float scaleY) {
		m00 *= scaleX;
		m01 *= scaleY;
		m10 *= scaleX;
		m11 *= scaleY;
		return this;
	}

	/** Postmultiplies this matrix with a scale matrix.
	 * @param scale The scale vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 scale (final Vector2 scale) {
		return scale(scale.x, scale.y);
	}

	/** Premultiplies this matrix with a scale matrix.
	 * @param scaleX The scale in the x-axis.
	 * @param scaleY The scale in the y-axis.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preScale (final float scaleX, final float scaleY) {
		m00 *= scaleX;
		m01 *= scaleX;
		m02 *= scaleX;
		m10 *= scaleY;
		m11 *= scaleY;
		m12 *= scaleY;
		return this;
	}

	/** Premultiplies this matrix with a scale matrix.
	 * @param scale The scale vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preScale (final Vector2 scale) {
		return preScale(scale.x, scale.y);
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
	 * @param degrees The angle in degrees
	 * @return This matrix for the purpose of chaining. */
	public Affine2 rotate (final float degrees) {
		if (degrees == 0) return this;

		final float cos = MathUtils.cosDeg(degrees);
		final float sin = MathUtils.sinDeg(degrees);

		final float tmp00 = m00 * cos + m01 * sin;
		final float tmp01 = m00 * -sin + m01 * cos;
		final float tmp10 = m10 * cos + m11 * sin;
		final float tmp11 = m10 * -sin + m11 * cos;

		m00 = tmp00;
		m01 = tmp01;
		m10 = tmp10;
		m11 = tmp11;
		return this;
	}

	/** Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
	 * @param radians The angle in radians
	 * @return This matrix for the purpose of chaining. */
	public Affine2 rotateRad (final float radians) {
		if (radians == 0) return this;

		final float cos = MathUtils.cos(radians);
		final float sin = MathUtils.sin(radians);

		final float tmp00 = m00 * cos + m01 * sin;
		final float tmp01 = m00 * -sin + m01 * cos;
		final float tmp10 = m10 * cos + m11 * sin;
		final float tmp11 = m10 * -sin + m11 * cos;

		m00 = tmp00;
		m01 = tmp01;
		m10 = tmp10;
		m11 = tmp11;
		return this;
	}

	/** Premultiplies this matrix with a (counter-clockwise) rotation matrix.
	 * @param degrees The angle in degrees
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preRotate (final float degrees) {
		if (degrees == 0) return this;

		final float cos = MathUtils.cosDeg(degrees);
		final float sin = MathUtils.sinDeg(degrees);

		final float tmp00 = cos * m00 - sin * m10;
		final float tmp01 = cos * m01 - sin * m11;
		final float tmp02 = cos * m02 - sin * m12;
		final float tmp10 = sin * m00 + cos * m10;
		final float tmp11 = sin * m01 + cos * m11;
		final float tmp12 = sin * m02 + cos * m12;

		m00 = tmp00;
		m01 = tmp01;
		m02 = tmp02;
		m10 = tmp10;
		m11 = tmp11;
		m12 = tmp12;
		return this;
	}

	/** Premultiplies this matrix with a (counter-clockwise) rotation matrix.
	 * @param radians The angle in radians
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preRotateRad (final float radians) {
		if (radians == 0) return this;

		final float cos = MathUtils.cos(radians);
		final float sin = MathUtils.sin(radians);

		final float tmp00 = cos * m00 - sin * m10;
		final float tmp01 = cos * m01 - sin * m11;
		final float tmp02 = cos * m02 - sin * m12;
		final float tmp10 = sin * m00 + cos * m10;
		final float tmp11 = sin * m01 + cos * m11;
		final float tmp12 = sin * m02 + cos * m12;

		m00 = tmp00;
		m01 = tmp01;
		m02 = tmp02;
		m10 = tmp10;
		m11 = tmp11;
		m12 = tmp12;
		return this;
	}

	/** Postmultiplies this matrix by a shear matrix.
	 * @param shearX The shear in x direction.
	 * @param shearY The shear in y direction.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 shear (final float shearX, final float shearY) {
		float tmp0 = m00 + shearY * m01;
		float tmp1 = m01 + shearX * m00;
		m00 = tmp0;
		m01 = tmp1;

		tmp0 = m10 + shearY * m11;
		tmp1 = m11 + shearX * m10;
		m10 = tmp0;
		m11 = tmp1;
		return this;
	}

	/** Postmultiplies this matrix by a shear matrix.
	 * @param shear The shear vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 shear (final Vector2 shear) {
		return shear(shear.x, shear.y);
	}

	/** Premultiplies this matrix by a shear matrix.
	 * @param shearX The shear in x direction.
	 * @param shearY The shear in y direction.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preShear (final float shearX, final float shearY) {
		final float tmp00 = m00 + shearX * m10;
		final float tmp01 = m01 + shearX * m11;
		final float tmp02 = m02 + shearX * m12;
		final float tmp10 = m10 + shearY * m00;
		final float tmp11 = m11 + shearY * m01;
		final float tmp12 = m12 + shearY * m02;

		m00 = tmp00;
		m01 = tmp01;
		m02 = tmp02;
		m10 = tmp10;
		m11 = tmp11;
		m12 = tmp12;
		return this;
	}

	/** Premultiplies this matrix by a shear matrix.
	 * @param shear The shear vector.
	 * @return This matrix for the purpose of chaining. */
	public Affine2 preShear (final Vector2 shear) {
		return preShear(shear.x, shear.y);
	}

	/** Calculates the determinant of the matrix.
	 * @return The determinant of this matrix. */
	public float det () {
		return m00 * m11 - m01 * m10;
	}

	/** Get the x-y translation component of the matrix.
	 * @param position Output vector.
	 * @return Filled position. */
	public Vector2 getTranslation (final Vector2 position) {
		position.x = m02;
		position.y = m12;
		return position;
	}

	/** Check if the this is a plain translation matrix.
	 * @return True if scale is 1 and rotation is 0. */
	public boolean isTranslation () {
		return (m00 == 1 && m11 == 1 && m01 == 0 && m10 == 0);
	}

	/** Check if this is an indentity matrix.
	 * @return True if scale is 1 and rotation is 0. */
	public boolean isIdt () {
		return (m00 == 1 && m02 == 0 && m12 == 0 && m11 == 1 && m01 == 0 && m10 == 0);
	}

	/** Applies the affine transformation on a vector. */
	public void applyTo (final Vector2 point) {
		final float x = point.x;
		final float y = point.y;
		point.x = m00 * x + m01 * y + m02;
		point.y = m10 * x + m11 * y + m12;
	}

	@Override
	public String toString () {
		return "[" + m00 + "|" + m01 + "|" + m02 + "]\n[" + m10 + "|" + m11 + "|" + m12 + "]\n[0.0|0.0|0.1]";
	}
}
