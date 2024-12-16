package net.micode.notes.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import net.micode.notes.R;
import jp.wasabeef.richeditor.RichEditor;

public class NotePreviewActivity extends Activity implements View.OnClickListener {
    private RichEditor mNotePreviewView;

    private static String mNoteText;

    private Button confirmButton;
    private Button cancelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View
        setContentView(R.layout.note_preview);

        // Set Components
        mNotePreviewView = (RichEditor) findViewById(R.id.note_preview_view);
        mNotePreviewView.setEditorHeight(500);
        mNotePreviewView.setEditorFontColor(Color.BLACK);
        mNotePreviewView.setInputEnabled(false);
        mNotePreviewView.setEditorWidth(LinearLayout.LayoutParams.MATCH_PARENT);

        mNoteText = getIntent().getStringExtra("note_text");
        mNotePreviewView.setHtml(mNoteText);

        confirmButton = (Button) findViewById(R.id.confirm_button);
        cancelButton = (Button) findViewById(R.id.cancel_button);
        confirmButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.confirm_button:
                Intent resultIntent = new Intent();
                resultIntent.putExtra("note_text", mNoteText);
                setResult(RESULT_OK, resultIntent);
            default:
                finish();
        }
    }
}
