package com.kovalexey.rdt1c.debug.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Notification
{
    public static void copyClipboard(String textClipboard)
    {
        Display display = getDisplay();

        display.asyncExec(() -> {
            Clipboard clipboard = new Clipboard(display);
            TextTransfer[] textTransfer = { TextTransfer.getInstance() };
            clipboard.setContents(new Object[] { textClipboard }, textTransfer);
            clipboard.dispose();
        });

    }
    
    public static void showMessage(String title, String textClipboard)
    {
        Display display = getDisplay();

        display.asyncExec(() -> {
        	MessageDialog.openInformation(new Shell(), title, textClipboard);			
        });

    }
    
    public static void showMessage(String textClipboard)
    {
        showMessage("RDT1CPlugin", textClipboard);
    }

    private static Display getDisplay()
    {
        Display display = Display.getCurrent();
        // может быть нулевым, если за пределами потока пользовательского интерфейса
        if (display == null)
            display = Display.getDefault();
        return display;
    }
}