/**
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.csipsimple.animation;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class Flip3dAnimation extends Animation {
	private Camera camera;
	private View view1;
	private View view2;
	private float centerX;
	private float centerY;
	private boolean forward;
	private boolean visibilitySwapped;

	/**
	 * Creates a 3D flip animation between two views. If forward is true, its
	 * assumed that view1 is "visible" and view2 is "gone" before the animation
	 * starts. At the end of the animation, view1 will be "gone" and view2 will
	 * be "visible". If forward is false, the reverse is assumed.
	 * 
	 * @param view1
	 *            First view in the transition.
	 * @param view2
	 *            Second view in the transition.
	 * @param centerX
	 *            The center of the views in the x-axis.
	 * @param centerY
	 *            The center of the views in the y-axis.
	 * @param forward
	 *            The direction of the animation.
	 */

	public Flip3dAnimation(View view1, View view2, int centerX, int centerY, boolean forward) {
		this.view1 = view1;
		this.view2 = view2;
		this.centerX = centerX;
		this.centerY = centerY;
		this.forward = forward;

		setDuration(1000);
		setFillAfter(true);
		setInterpolator(new AccelerateDecelerateInterpolator());
	}

	@Override
	public void initialize(int width, int height, int parentWidth, int parentHeight) {
		super.initialize(width, height, parentWidth, parentHeight);

		camera = new Camera();
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		// Angle around the y-axis of the rotation at the given time. It is
		// calculated both in radians and in the equivalent degrees.
		final double radians = Math.PI * interpolatedTime;
		float degrees = (float) (180.0 * radians / Math.PI);

		// Once we reach the midpoint in the animation, we need to hide the
		// source view and show the destination view. We also need to change
		// the angle by 180 degrees so that the destination does not come in
		// flipped around. This is the main problem with SDK sample, it does not
		// do this.
		if (interpolatedTime >= 0.5f) {
			degrees -= 180.f;

			if (!visibilitySwapped) {
				if (forward) {
					view1.setVisibility(View.GONE);
					view2.setVisibility(View.VISIBLE);
				} else {
					view2.setVisibility(View.GONE);
					view1.setVisibility(View.VISIBLE);
				}

				visibilitySwapped = true;
			}
		}

		if (!forward) {
			degrees = -degrees;
		}
		final Matrix matrix = t.getMatrix();

		camera.save();
		camera.translate(0.0f, 0.0f, (float) (310.0 * Math.sin(radians)));
		camera.rotateY(degrees);
		camera.getMatrix(matrix);
		camera.restore();

		matrix.preTranslate(-centerX, -centerY);
		matrix.postTranslate(centerX, centerY);
	}
}
