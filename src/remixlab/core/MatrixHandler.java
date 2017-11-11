/**************************************************************************************
 * dandelion_tree
 * Copyright (c) 2014-2017 National University of Colombia, https://github.com/remixlab
 * @author Jean Pierre Charalambos, http://otrolado.info/
 *
 * All rights reserved. Library that eases the creation of interactive
 * scenes, released under the terms of the GNU Public License v3.0
 * which is available at http://www.gnu.org/licenses/gpl.html
 **************************************************************************************/

package remixlab.core;

import remixlab.primitives.Matrix;
import remixlab.primitives.Vector;

/**
 * Various matrix operations dandelion should support either through a third-party
 * implementation or locally.
 */
public class MatrixHandler {
  protected Graph _graph;
  protected Matrix _projection, _view, _modelview;
  protected Matrix _projectionView, _projectionViewInverse;
  protected boolean _isProjectionViewInverseCached, _projectionViewHasInverse;

  public static int STACK_DEPTH = 32;
  public static String ERROR_PUSHMATRIX_OVERFLOW = "Too many calls to pushModelView().";
  public static String ERROR_PUSHMATRIX_UNDERFLOW = "Too many calls to popModelView(), and not enough to pushModelView().";
  protected float[][] _stack = new float[STACK_DEPTH][16];
  protected int _stackDepth;
  protected float[][] pmatrixStack = new float[STACK_DEPTH][16];
  protected int pmatrixStackDepth;

  /**
   * Instantiates matrices and sets
   * {@link #isProjectionViewInverseCached()} to {@code false}.
   *
   * @param scn
   */
  public MatrixHandler(Graph scn) {
    _graph = scn;
    _projection = new Matrix();
    _view = new Matrix();
    _modelview = new Matrix();
    _projectionView = new Matrix();
    _isProjectionViewInverseCached = false;
  }

  /**
   * Returns the graph this object belongs to
   */
  public Graph graph() {
    return _graph;
  }

  /**
   * Binds matrices to (raster) renderer.
   */
  public void bind() {
    cacheProjection(graph().computeProjection());
    cacheView(graph().computeView());
    cacheProjectionView(Matrix.multiply(cacheProjection(), cacheView()));
    bindProjection(cacheProjection());
    bindModelView(cacheView());
  }

  /**
   * @return projection matrix
   */
  public Matrix projection() {
    return cacheProjection();
  }

  /**
   * Binds the projection matrix to the renderer.
   */
  public void bindProjection(Matrix matrix) {
    cacheProjection(matrix);
  }

  /**
   * Caches the projection matrix.
   */
  public void cacheProjection(Matrix matrix) {
    _projection.set(matrix);
  }

  /**
   * Returns the cached projection matrix.
   */
  public Matrix cacheProjection() {
    return _projection;
  }

  /**
   * @return view matrix
   */
  public Matrix view() {
    return cacheView();
  }

  /**
   * Binds the view matrix to the renderer.
   */
  public void bindView(Matrix matrix) {
    cacheView(matrix);
  }

  /**
   * Caches the view matrix.
   */
  public void cacheView(Matrix matrix) {
    _view.set(matrix);
  }

  /**
   * Returns the cached view matrix.
   */
  public Matrix cacheView() {
    return _view;
  }

  /**
   * @return modelview matrix
   */
  public Matrix modelView() {
    return _modelview;
  }

  /**
   * Binds the modelview matrix to the renderer.
   */
  public void bindModelView(Matrix matrix) {
    _modelview.set(matrix);
  }

  /**
   * Caches the projection * view matrix.
   *
   * @see #isProjectionViewInverseCached()
   */
  public void cacheProjectionView(Matrix matrix) {
    _projectionView.set(matrix);
    if (isProjectionViewInverseCached()) {
      if (_projectionViewInverse == null)
        _projectionViewInverse = new Matrix();
      _projectionViewHasInverse = _projectionView.invert(_projectionViewInverse);
    }
  }

  /**
   * Returns the cached projection times view matrix.
   */
  public Matrix cacheProjectionView() {
    return _projectionView;
  }

  /**
   * Returns {@code true} if {@code P x M} and {@code inv (P x M)} are being cached, and
   * {@code false} otherwise.
   *
   * @see #cacheProjectionView()
   * @see #cacheProjectionViewInverse(boolean)
   */
  public boolean isProjectionViewInverseCached() {
    return _isProjectionViewInverseCached;
  }

  /**
   * Cache {@code inv (P x M)} (and also {@code (P x M)} ) so that
   * {@link Graph#unprojectedCoordinatesOf(Vector)} is
   * optimized.
   *
   * @see #isProjectionViewInverseCached()
   * @see #cacheProjectionView()
   */
  public void cacheProjectionViewInverse(boolean optimise) {
    _isProjectionViewInverseCached = optimise;
  }

  /**
   * Returns the cached projection times view inverse matrix.
   */
  public Matrix cacheProjectionViewInverse() {
    if (!isProjectionViewInverseCached())
      throw new RuntimeException("optimizeUnprojectCache(true) should be called first");
    return _projectionViewInverse;
  }

  /**
   * Multiplies the current modelview matrix by the one specified through the parameters.
   */
  public void applyModelView(Matrix source) {
    _modelview.apply(source);
  }

  /**
   * Multiplies the current projection matrix by the one specified through the parameters.
   */
  public void applyProjection(Matrix source) {
    _projection.apply(source);
  }

  /**
   * Push a copy of the modelview matrix onto the stack.
   */
  public void pushModelView() {
    if (_stackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _modelview.get(_stack[_stackDepth]);
    _stackDepth++;
  }

  /**
   * Replace the current modelview matrix with the top of the stack.
   */
  public void popModelView() {
    if (_stackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    _stackDepth--;
    _modelview.set(_stack[_stackDepth]);
  }

  /**
   * Translate in X and Y.
   */
  public void translate(float tx, float ty) {
    translate(tx, ty, 0);
  }

  /**
   * Translate in X, Y, and Z.
   */
  public void translate(float tx, float ty, float tz) {
    _modelview.translate(tx, ty, tz);
  }

  /**
   * Two dimensional rotation.
   * <p>
   * Same as _rotateZ (this is identical to a 3D rotation along the z-axis) but included
   * for clarity. It'd be weird for people drawing 2D graphics to be using _rotateZ. And
   * they might kick our a-- for the confusion.
   * <p>
   * <A HREF="http://www.xkcd.com/c184.html">Additional background</A>.
   */
  public void rotate(float angle) {
    rotateZ(angle);
  }

  /**
   * Rotate around the X axis.
   */
  public void rotateX(float angle) {
    _modelview.rotateX(angle);
  }

  /**
   * Rotate around the Y axis.
   */
  public void rotateY(float angle) {
    _modelview.rotateY(angle);
  }

  /**
   * Rotate around the Z axis.
   */
  public void rotateZ(float angle) {
    _modelview.rotateZ(angle);
  }

  /**
   * Rotate about a vector in space. Same as the glRotatef() function.
   */
  public void rotate(float angle, float v0, float v1, float v2) {
    _modelview.rotate(angle, v0, v1, v2);
  }

  /**
   * Scale equally in all dimensions.
   */
  public void scale(float s) {
    scale(s, s, s);
  }

  /**
   * Scale in X and Y. Equivalent to _scale(sx, sy, 1).
   * <p>
   * Not recommended for use in 3D, because the z-dimension is just scaled by 1, since
   * there's no way to know what else to _scale it by.
   */
  public void scale(float sx, float sy) {
    scale(sx, sy, 1);
  }

  /**
   * Scale in X, Y, and Z.
   */
  public void scale(float x, float y, float z) {
    _modelview.scale(x, y, z);
  }

  /**
   * Push a copy of the projection matrix onto the stack.
   */
  public void pushProjection() {
    if (pmatrixStackDepth == STACK_DEPTH) {
      throw new RuntimeException(ERROR_PUSHMATRIX_OVERFLOW);
    }
    _projection.get(pmatrixStack[pmatrixStackDepth]);
    pmatrixStackDepth++;
  }

  /**
   * Replace the current projection matrix with the top of the stack.
   */
  public void popProjection() {
    if (pmatrixStackDepth == 0) {
      throw new RuntimeException(ERROR_PUSHMATRIX_UNDERFLOW);
    }
    pmatrixStackDepth--;
    _projection.set(pmatrixStack[pmatrixStackDepth]);
  }

  /**
   * Computes the world coordinates of an screen object so that drawing can be done
   * directly with 2D screen coordinates.
   * <p>
   * All screen drawing should be enclosed between {@link #beginScreenDrawing()} and
   * {@link #endScreenDrawing()}. Then you can just begin drawing your screen shapes.
   * <b>Attention:</b> If you want your screen drawing to appear on top of your 3d graph
   * then draw first all your 3d before doing any call to a {@link #beginScreenDrawing()}
   * and {@link #endScreenDrawing()} pair.
   *
   * @see #endScreenDrawing()
   */
  public void beginScreenDrawing() {
    pushProjection();
    ortho2D();
    pushModelView();
    resetViewPoint();
  }

  /**
   * Ends screen drawing. See {@link #beginScreenDrawing()} for details.
   *
   * @see #beginScreenDrawing()
   */
  public void endScreenDrawing() {
    popProjection();
    popModelView();
  }

  // see:
  // http://www.opengl.org/archives/resources/faq/technical/transformations.htm
  // "9.030 How do I draw 2D controls over my 3D rendering?"
  protected void ortho2D() {
    float cameraZ = (_graph.height() / 2.0f) / (float) Math.tan((float) Math.PI / 8);
    float cameraNear = cameraZ / 2.0f;
    float cameraFar = cameraZ * 2.0f;

    float left = -_graph.width() / 2;
    float right = _graph.width() / 2;
    float bottom = -_graph.height() / 2;
    float top = _graph.height() / 2;
    float near = cameraNear;
    float far = cameraFar;

    float x = +2.0f / (right - left);
    float y = +2.0f / (top - bottom);
    float z = -2.0f / (far - near);

    float tx = -(right + left) / (right - left);
    float ty = -(top + bottom) / (top - bottom);
    float tz = -(far + near) / (far - near);

    // The minus sign is needed to invert the Y axis.
    bindProjection(new Matrix(x, 0, 0, 0, 0, -y, 0, 0, 0, 0, z, 0, tx, ty, tz, 1));
  }

  // as it's done in P5:
  protected void resetViewPoint() {
    float eyeX = _graph.width() / 2f;
    float eyeY = _graph.height() / 2f;
    float eyeZ = (_graph.height() / 2f) / (float) Math.tan((float) Math.PI * 60 / 360);
    float centerX = _graph.width() / 2f;
    float centerY = _graph.height() / 2f;
    float centerZ = 0;
    float upX = 0;
    float upY = 1;
    float upZ = 0;

    // Calculating Z vector
    float z0 = eyeX - centerX;
    float z1 = eyeY - centerY;
    float z2 = eyeZ - centerZ;
    float mag = (float) Math.sqrt(z0 * z0 + z1 * z1 + z2 * z2);
    if (mag != 0) {
      z0 /= mag;
      z1 /= mag;
      z2 /= mag;
    }

    // Calculating Y vector
    float y0 = upX;
    float y1 = upY;
    float y2 = upZ;

    // Computing X vector as Y cross Z
    float x0 = y1 * z2 - y2 * z1;
    float x1 = -y0 * z2 + y2 * z0;
    float x2 = y0 * z1 - y1 * z0;

    // Recompute Y = Z cross X
    y0 = z1 * x2 - z2 * x1;
    y1 = -z0 * x2 + z2 * x0;
    y2 = z0 * x1 - z1 * x0;

    // Cross product gives area of parallelogram, which is < 1.0 for
    // non-perpendicular unit-length vectors; so normalize x, y here:
    mag = (float) Math.sqrt(x0 * x0 + x1 * x1 + x2 * x2);
    if (mag != 0) {
      x0 /= mag;
      x1 /= mag;
      x2 /= mag;
    }

    mag = (float) Math.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
    if (mag != 0) {
      y0 /= mag;
      y1 /= mag;
      y2 /= mag;
    }

    Matrix mv = new Matrix(x0, y0, z0, 0, x1, y1, z1, 0, x2, y2, z2, 0, 0, 0, 0, 1);

    float tx = -eyeX;
    float ty = -eyeY;
    float tz = -eyeZ;

    mv.translate(tx, ty, tz);
    bindModelView(mv);
  }
}
