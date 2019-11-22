package origamieditor3d.unittest;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import origamieditor3d.graphics.Camera;
import origamieditor3d.origami.Geometry;
import origamieditor3d.ui.panel.OrigamiPanel;
import origamieditor3d.ui.panel.Panel.RulerMode;

public class OrigamiPanelTest {

    @Test
    public void testRulerOn() {
       Camera refcam = new Camera(0, 0, 1);
       OrigamiPanel origamiPanel= new OrigamiPanel();
       origamiPanel.rulerOn(refcam, 1, 2, 3, 4);
       double [] vector = origamiPanel.getRulerNormalvector();
       double[] vectorTest= new double[] {
                       refcam.getXScale()[0] * (4 - 2)
                               + refcam.getYScale()[0] * (1 - 3),
                      refcam.getXScale()[1] * (4 - 2)
                               + refcam.getYScale()[1] * (1 - 3),
                       refcam.getXScale()[2] * (4 - 2)
                               + refcam.getYScale()[2] * (1 - 3) };

       if (Geometry.scalarProduct(refcam.getCamPosition(), vectorTest)
                       - Geometry.scalarProduct(origamiPanel.getRulerPoint(), vectorTest) > 0) {
                   vectorTest = new double[] { -vectorTest[0], -vectorTest[1], -vectorTest[2] };
               }
       
       Arrays.equals(vector, vectorTest);
              
    }

}
