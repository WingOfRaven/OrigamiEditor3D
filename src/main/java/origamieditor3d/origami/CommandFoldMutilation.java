package origamieditor3d.origami;

public class CommandFoldMutilation extends CommandFold {
	static public final int foldID = 6;

	public CommandFoldMutilation(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {
		super(origami);
		this.ppoint = ppoint;
		this.pnormal = pnormal;
	}


	@Override
	public int getFoldID() {
		return foldID;
	}

	@Override
	public void execute() {
		origami.internalMutilation(ppoint, pnormal);
	}

	@Override
	public void undo() {}
}

