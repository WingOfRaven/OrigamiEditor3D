package origamieditor3d.origami;

public class CommandFoldMutilation extends CommandFold {
	
	
	static {
		foldId = 6;
	}
	public CommandFoldMutilation(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.origami = origami;
		this.foldId = 6;

	}
	
	public void execute() {
		origami.internalMutilation(ppoint, pnormal);
	}
	public void undo() {}
	


}

