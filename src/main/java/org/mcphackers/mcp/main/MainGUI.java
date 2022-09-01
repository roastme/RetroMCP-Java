package org.mcphackers.mcp.main;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.gui.MCPFrame;
import org.mcphackers.mcp.gui.TaskButton;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;
import org.mcphackers.mcp.tools.versions.json.Version;

/**
 * GUI implementation of MCP
 */
public class MainGUI extends MCP {
	public Path workingDir;
	public MCPFrame frame;
	public boolean isActive = true;
	public Options options;
	public Version currentVersion;
	
	public static final TaskMode[] TASKS = {TaskMode.DECOMPILE, TaskMode.RECOMPILE, TaskMode.REOBFUSCATE, TaskMode.BUILD, TaskMode.UPDATE_MD5, TaskMode.CREATE_PATCH};
	
	public static void main(String[] args) throws Exception {
		Path workingDir = Paths.get("");
		if(args.length >= 1) {
			try {
				workingDir = Paths.get(args[0]);
			} catch (InvalidPathException e) {}
		}
		new MainGUI(workingDir);
	}
	
	public MainGUI(Path dir) {
		workingDir = dir;
		options = new Options(MCPPaths.get(this, "options.cfg"));
		if(options.lang != null) {
			changeLanguage(options.lang);
		}
		JavaCompiler c = ToolProvider.getSystemJavaCompiler();
		if (c == null) {
			JOptionPane.showMessageDialog(null, TRANSLATOR.translateKey("mcp.needJDK"), TRANSLATOR.translateKey("mcp.error"), JOptionPane.ERROR_MESSAGE);
		}
		Path versionPath = MCPPaths.get(this, MCPPaths.VERSION);
		if(Files.exists(versionPath)) {
			try {
				currentVersion = Version.from(new JSONObject(new String(Files.readAllBytes(versionPath))));
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
		}
		frame = new MCPFrame(this);
	}
	
	public Side getSide() {
		return getOptions().side;
	}
	
	@Override
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
		frame.setProgressBars(tasks, mode);
	}

	@Override
	public void clearProgressBars() {
		frame.resetProgressBars();
	}

	public void setActive(boolean active) {
		isActive = active;
		if(active) {
			frame.updateButtonState();
		}
		else {
			frame.setAllButtonsInactive();
		}
	}

	@Override
	public Version getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public Options getOptions() {
		return options;
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		frame.setProgress(side, progressMessage);
	}

	@Override
	public void setProgress(int side, int progress) {
		frame.setProgress(side, progress);
	}

	@Override
	public boolean yesNoInput(String title, String msg) {
		//frame.setExtendedState(Frame.NORMAL);
		return JOptionPane.showConfirmDialog(frame, msg, title, JOptionPane.YES_NO_OPTION) == 0;
	}

	@Override
	public String inputString(String title, String msg) {
		//frame.setExtendedState(Frame.NORMAL);
		return JOptionPane.showInputDialog(frame, msg, title, JOptionPane.PLAIN_MESSAGE);
	}

	public void showMessage(String title, String msg, int type) {
		//frame.setExtendedState(Frame.NORMAL);
		switch (type) {
		case Task.INFO:
			type = JOptionPane.INFORMATION_MESSAGE;
			break;
		case Task.WARNING:
			type = JOptionPane.WARNING_MESSAGE;
			break;
		case Task.ERROR:
			type = JOptionPane.ERROR_MESSAGE;
			break;
		}
		JOptionPane.showMessageDialog(frame, msg, title, type);
	}

	@Override
	public void setCurrentVersion(Version version) {
		currentVersion = version;
		frame.setCurrentVersion(version == null ? null : VersionParser.INSTANCE.getVersion(version.id));
	}

	@Override
	public Path getWorkingDir() {
		return workingDir;
	}

	@Override
	public boolean updateDialogue(String changelog, String version) {
		JPanel outer = new JPanel(new BorderLayout());
		JPanel components = new JPanel();
		components.setLayout(new BoxLayout(components, BoxLayout.Y_AXIS));
		String[] lines = changelog.split("\n");
		for(String line : lines) {
			line = line.replace("`", "");
			char bullet = '\u2022';
			if(line.startsWith("# ")) {
				JLabel label = new JLabel(line.substring(2));
				label.setBorder(new EmptyBorder(0, 0, 4, 0));
				label.setFont(label.getFont().deriveFont(22F));
				components.add(label);
			}
			else if(line.startsWith("-"))
			{
				JLabel label = new JLabel(bullet + " " + line.substring(1));
				label.setFont(label.getFont().deriveFont(Font.PLAIN).deriveFont(14F));
				components.add(label);
			}
			else if(line.startsWith("  -"))
			{
				JLabel label = new JLabel(bullet + " " + line.substring(3));
				label.setFont(label.getFont().deriveFont(Font.PLAIN).deriveFont(14F));
				label.setBorder(new EmptyBorder(0, 12, 0, 0));
				components.add(label);
			}
			else {
				components.add(new JLabel(line));
			}
		}
		outer.add(components);
		JLabel label = new JLabel(TRANSLATOR.translateKey("mcp.confirmUpdate"));
		label.setFont(label.getFont().deriveFont(14F));
		label.setBorder(new EmptyBorder(10, 0, 0, 0));
		label.setHorizontalAlignment(SwingConstants.CENTER);
		outer.add(label, BorderLayout.SOUTH);
		return JOptionPane.showConfirmDialog(frame, outer, TRANSLATOR.translateKey("mcp.newVersion") + version, JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE) == 0;
	}

	public void setupVersion(VersionData versionData) {
		Version version = getCurrentVersion();
		if (versionData != null && !versionData.equals(version == null ? null : VersionParser.INSTANCE.getVersion(version.id))) {
			int response = JOptionPane.showConfirmDialog(frame, MCP.TRANSLATOR.translateKey("mcp.confirmSetup"), MCP.TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
			switch (response) {
				case 0:
					setParameter(TaskParameter.SETUP_VERSION, versionData.id);
					performTask(TaskMode.SETUP, Side.ANY);
					break;
				default:
					frame.setCurrentVersion(VersionParser.INSTANCE.getVersion(version == null ? null : version.id));
					break;
			}
		}
	}

	public TaskButton getButton(TaskMode task) {
		TaskButton button;
		if(task == TaskMode.DECOMPILE) {
			ActionListener defaultActionListener = event -> operateOnThread(() -> {
				int response = JOptionPane.YES_OPTION;
				if(TaskMode.RECOMPILE.isAvailable(this, getSide())) {
					response = JOptionPane.showConfirmDialog(frame, TRANSLATOR.translateKey("mcp.confirmDecompile"), TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
					if(response == JOptionPane.YES_OPTION) {
						int response2 = JOptionPane.showConfirmDialog(frame, TRANSLATOR.translateKey("mcp.askSourceBackup"), TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_CANCEL_OPTION);
						if(response2 == JOptionPane.YES_OPTION) {
							performTask(TaskMode.BACKUP_SRC, getSide());
						}
						else if(response2 != JOptionPane.NO_OPTION) {
							response = response2;
						}
					}
				}
				if(response == JOptionPane.YES_OPTION) {
					performTask(TaskMode.DECOMPILE, getSide());
				}
			});
			button = new TaskButton(this, task, defaultActionListener);
		}
		else if(task == TaskMode.UPDATE_MD5) {
			ActionListener defaultActionListener = event -> operateOnThread(() -> {
				int response = JOptionPane.showConfirmDialog(frame, TRANSLATOR.translateKey("mcp.confirmUpdateMD5"), TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
				if(response == JOptionPane.YES_OPTION) {
					performTask(task, getSide());
				}
			});
			button = new TaskButton(this, task, defaultActionListener);
		}
		else {
			button = new TaskButton(this, task);
		}
		button.setEnabled(false);
		return button;
	}
}
