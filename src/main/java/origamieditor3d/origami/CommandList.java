package origamieditor3d.origami;

import java.util.LinkedList;

public class CommandList {

	/** The commands. */
	private LinkedList<CommandFold> commands;
	
	/** The index. */
	private int index;
	
	
	/** The window. */

	/**
	 * Instantiates a new CommandList.
	 * @param window the window
	 */
	public CommandList() {

		commands = new LinkedList<>();
		index = -1;
	}

	/**
	 * Instantiates a new CommandList.
	 * @param window the window
	 */
	public CommandList(CommandList commandList, OrigamiGen1 newOrigami) {

		commands = new LinkedList<>();
		for (CommandFold command : commandList.commands) {
		    try {
		        CommandFold newCommand = (CommandFold) command.clone();
		        newCommand.setOrigami(newOrigami);
                commands.add(newCommand);
            } catch (CloneNotSupportedException e) {
		        throw new RuntimeException(e);
            }
        }
		index = commandList.index;
	}
	
	
	/**
	 * Adds a command.
	 *
	 * @param command the command to add
	 */
	public void addCommand(CommandFold command, boolean execute) {
		int i = index+1;
		while(i<commands.size()) {
			commands.remove(i);
		}
		index++;
		commands.add(command);
		if (execute) {
			command.execute();
		}
	}
	
	
	/**
	 * Undo the command.
	 */
	public void undo() {
		if(index >= 0)
		{
			commands.get(index).undo();
			index--;
		} 
	
	}
	
	/**
	 * Redo the command.
	 */
	public void redo() {
		if(index<commands.size()-1)
		{
			index++;
			commands.get(index).execute();
		}
	
	}
	
	/**
	 * Reset the list.
	 */
	public void reset() {
		index = -1;
		commands.clear();
	
	}
	
	public void redoAll() {
		if(index<commands.size()-1)
		{
			index++;
			this.redo();
		}
	
	}

	public int getSize() {
		return commands.size();
	}
	
	public void execute(int index) {
		commands.get(index).execute();
	}

	public CommandFold get(int index) { return commands.get(index);	}

	public void clearRange(int start, int end) {
		commands.subList(start, end).clear();
	}
}
