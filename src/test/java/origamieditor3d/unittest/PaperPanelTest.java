package origamieditor3d.unittest;

import static org.junit.Assert.*;

import java.util.Arrays;

import origamieditor3d.ui.panel.PaperPanel;
import origamieditor3d.graphics.Camera;

import org.junit.Test;

public class PaperPanelTest {

    @Test
    public void testSetTracker() {
        PaperPanel paperPanel = new PaperPanel();
        paperPanel.setTracker(new Camera(0,0,1), 1, 2);
        assertEquals(1, (int) paperPanel.tracker_x());
        assertEquals(2, (int) paperPanel.tracker_y());
        assertEquals(true, paperPanel.isTracked());
    }
    
    @Test
    public void testResetTracker() {
        PaperPanel paperPanel = new PaperPanel();
        paperPanel.resetTracker();
        assertEquals(null, paperPanel.tracker_x());
        assertEquals(null, paperPanel.tracker_y());
        assertEquals(false, paperPanel.isTracked());
    } 
    
    @Test
    public void testTiltTriangle() {
        PaperPanel paperPanel = new PaperPanel();
        paperPanel.tiltTriangleTo(new Camera (0,0,1), 1,2,3);
        Arrays.equals(new Integer[] {1,2,3}, paperPanel.linerTriangle()[0]);
    }
    
    @Test   
    public void testResetTriangle() {
        PaperPanel paperPanel = new PaperPanel();
        paperPanel.tiltTriangleTo(new Camera (0,0,1), 1,2,3);
        paperPanel.resetTriangle();
        Arrays.equals(null, paperPanel.linerTriangle()[0]);
    }
    
    @Test    
    public void testReset() {
        PaperPanel paperPanel = new PaperPanel();
        paperPanel.setTracker(new Camera(0,0,1), 1, 2);
        paperPanel.tiltTriangleTo(new Camera (0,0,1), 1,2,3);
        paperPanel.reset();
        Arrays.equals(null, paperPanel.linerTriangle()[0]);
        assertEquals(null, paperPanel.tracker_x());
        assertEquals(null, paperPanel.tracker_y());
        assertEquals(false, paperPanel.isTracked());
    }
    
    
}
