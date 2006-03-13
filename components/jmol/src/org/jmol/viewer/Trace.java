/* $RCSfile: Trace.java,v $
 * $Author: mhall $
 * $Date: 2006-03-13 18:03:17 $
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

class Trace extends Mps {

  Mps.Mpspolymer allocateMpspolymer(Polymer polymer) {
    return new Tchain(polymer);
  }

  class Tchain extends Mps.Mpspolymer {
    Tchain(Polymer polymer) {
      super(polymer, 600, 1500, 500, 1500);
    }
  }
}

