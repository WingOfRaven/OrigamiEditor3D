package origamieditor3d.origami;

public class CommandFoldReflection extends CommandFold {
	static public final int foldID = 1;

	public CommandFoldReflection(double[] ppoint, double[] pnormal, OrigamiGen1 origami) {
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
		origami.internalReflectionFold(ppoint, pnormal);
	}

	@Override
	public void undo() {}
}

