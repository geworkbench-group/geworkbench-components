/* $RCSfile: FrameRenderer.java,v $
 * $Author: wangm $
 * $Date: 2008-12-09 17:00:32 $
 * $Revision: 1.1 $
 *
 * Copyright (C) 2003-2004  The Jmol Development Team
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import org.jmol.g3d.*;
import javax.vecmath.Point3i;
import java.awt.Rectangle;

import java.awt.FontMetrics;

class FrameRenderer {

  Viewer viewer;

  ShapeRenderer[] renderers = new ShapeRenderer[JmolConstants.SHAPE_MAX];

  FrameRenderer(Viewer viewer) {
    this.viewer = viewer;
  }

  void render(Graphics3D g3d, Rectangle rectClip,
                     Frame frame, int displayModelIndex) {

    if (frame == null || frame.atomCount <= 0)
      return;

    viewer.calcTransformMatrices();

    for (int i = 0; i < JmolConstants.SHAPE_MAX; ++i) {
      Shape shape = frame.shapes[i];
      if (shape == null)
        continue;
      getRenderer(i, g3d).render(g3d, rectClip, frame,
                                 displayModelIndex, shape);
    }
  }

  ShapeRenderer getRenderer(int refShape, Graphics3D g3d) {
    if (renderers[refShape] == null)
      renderers[refShape] = allocateRenderer(refShape, g3d);
    return renderers[refShape];
  }

  ShapeRenderer allocateRenderer(int refShape, Graphics3D g3d) {
    String classBase =
      JmolConstants.shapeClassBases[refShape] + "Renderer";
    String className = "org.jmol.viewer." + classBase;

    try {
      Class shapeClass = Class.forName(className);
      ShapeRenderer renderer = (ShapeRenderer)shapeClass.newInstance();
      renderer.setViewerFrameRenderer(viewer, this, g3d);
      return renderer;
    } catch (Exception e) {
      System.out.println("Could not instantiate renderer:" + classBase +
                         "\n" + e);
      e.printStackTrace();
    }
    return null;
  }

  void renderStringOutside(String str, short colix, Font3D font3d,
                                  Point3i screen, Graphics3D g3d) {
    renderStringOutside(str, colix, font3d,
                        screen.x, screen.y, screen.z, g3d);
  }

  void renderStringOutside(String str, short colix, Font3D font3d,
                                  int x, int y, int z, Graphics3D g3d) {
    g3d.setColix(colix);
    g3d.setFont(font3d);
    FontMetrics fontMetrics = font3d.fontMetrics;
    int strAscent = fontMetrics.getAscent();
    int strWidth = fontMetrics.stringWidth(str);
    int xStrCenter, yStrCenter;
    int xCenter = viewer.getBoundingBoxCenterX();
    int yCenter = viewer.getBoundingBoxCenterY();
    int dx = x - xCenter;
    int dy = y - yCenter;
    if (dx == 0 && dy == 0) {
      xStrCenter = x;
      yStrCenter = y;
    } else {
      int dist = (int) Math.sqrt(dx*dx + dy*dy);
      xStrCenter = xCenter + ((dist + 2 + (strWidth + 1) / 2) * dx / dist);
      yStrCenter = yCenter + ((dist + 3 + (strAscent + 1)/ 2) * dy / dist);
    }
    int xStrBaseline = xStrCenter - strWidth / 2;
    int yStrBaseline = yStrCenter + strAscent / 2;
    g3d.drawString(str, colix, xStrBaseline, yStrBaseline, z);
  }
}
