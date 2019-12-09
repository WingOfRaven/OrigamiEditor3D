package origamieditor3d.origami;

public class CommandFoldMutilation extends CommandFold {
	static {
		foldID = 6;
	}

	public CommandFoldMutilation(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {

		this.ppoint = ppoint;
		this.pnormal = pnormal;
		this.origami = origami;
	}
	
	public void execute() {
		origami.internalMutilation(ppoint, pnormal);
	}
	public void undo() {}
}

