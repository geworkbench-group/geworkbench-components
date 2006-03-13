/* $RCSfile: Backbone.java,v $
 * $Author: mhall $
 * $Date: 2006-03-13 18:03:15 $
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
 *  Lesser General License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 */

package org.jmol.viewer;

import java.util.BitSet;

class Backbone extends Mps {

  Mps.Mpspolymer allocateMpspolymer(Polymer polymer) {
    return new Bbpolymer(polymer);
  }

  class Bbpolymer extends Mps.Mpspolymer {

    Bbpolymer(Polymer polymer) {
      super(polymer, 1, 1500, 500, 2000);
    }

    void setMad(short mad, BitSet bsSelected) {
      boolean bondSelectionModeOr = viewer.getBondSelectionModeOr();
      int[] atomIndices = polymer.getLeadAtomIndices();
      // note that i is initialized to monomerCount - 1
      // in order to skip the last atom
      // but it is picked up within the loop by looking at i+1
      for (int i = monomerCount - 1; --i >= 0; ) {
        if ((bsSelected.get(atomIndices[i]) &&
             bsSelected.get(atomIndices[i + 1]))
            ||
            (bondSelectionModeOr &&
             (bsSelected.get(atomIndices[i]) ||
              bsSelected.get(atomIndices[i + 1]))))
          mads[i] = mad;
      }
    }
  }
}
