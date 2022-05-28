package org.mcphackers.mcp.gui;

import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextArea;

public class TextAreaOutputStream extends PrintStream {
	private final JTextArea textArea;

	public TextAreaOutputStream(JTextArea textArea, OutputStream out) {
		super(out, true);
    	this.textArea = textArea;
	}

    @Override
    public void print(String s) {
		textArea.append(s);
        super.print(s);
    }

    @Override
    public void println(String s) {
        super.println(s);
		textArea.append("\n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
    }
}