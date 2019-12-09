package origamieditor3d.origami;

public class CommandFoldReflection extends CommandFold {
	
	
	static {
		foldId = 1;
	}
	public CommandFoldReflection(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.origami = origami;

	}
	public void execute() {
		origami.internalReflectionFold(ppoint, pnormal);
	}
	public void undo() {}

}

