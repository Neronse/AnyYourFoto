package com.foracademy.auditore.anyyourfoto;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;

import com.foracademy.auditore.motofoto.R;

public class DialogAboutApp extends DialogFragment {

    public static DialogAboutApp newInstance() {
        return new DialogAboutApp();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_about_app, null);
        builder.setView(view);
        builder.setTitle(R.string.dialog_about_title);
        builder.setPositiveButton("Ok", (dialog, which) -> dialog.dismiss());
        return builder.create();
    }
}
