package origamieditor3d.unittest;

import junit.framework.*;
import origamieditor3d.script.*;

public class UnitTest extends TestCase{
	
	public void testGood() throws Exception {
		assertEquals("paper [test] ",OrigamiScripter.paper("test"));
	}
	
	public void testBad() throws Exception {
		assertEquals("paper [test] ",OrigamiScripter.paper("testOops"));
	}

}
