/**
 * This file is part of TKConfig application.
 * 
 * Copyright (C) 2013 Claudiu Ciobotariu
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.ciubex.tkconfig.activities;

import ro.ciubex.tkconfig.R;
import ro.ciubex.tkconfig.dialogs.EditorDialog;
import ro.ciubex.tkconfig.dialogs.ParameterEditor;
import ro.ciubex.tkconfig.list.CommandListAdapter;
import ro.ciubex.tkconfig.models.Command;
import ro.ciubex.tkconfig.models.Constants;
import ro.ciubex.tkconfig.models.GpsContact;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

/**
 * The main activity which should load and show the commands.
 * 
 * @author Claudiu Ciobotariu
 * 
 */
public class TKConfigActivity extends BaseActivity {
	private CommandListAdapter adapter;
	private ListView commandsList;

	private final int CONFIRM_ID_DELETE = 0;
	private final int CONFIRM_ID_SMS_SEND = 1;
	private final int CONFIRM_ID_PARAMETERS = 2;
	private final int CONFIRM_ID_DONATE = 3;
	private final int SMS_NO_CONTACT = 4;

	private static final int REQUEST_CODE_SETTINGS = 0;
	private static final int REQUEST_CODE_ABOUT = 1;

	/**
	 * The method invoked when the activity is creating
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_config);
		setMenuId(R.menu.activity_config);
		prepareMainListView();
	}

	/**
	 * Method invoked when the activity is started.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		app.showProgressDialog(this, R.string.please_wait);
		app.commandsLoad();
		reloadAdapter();
	}

	/**
	 * Invoked when the activity is put on pause
	 */
	@Override
	protected void onPause() {
		app.onClose();
		super.onPause();
	}

	/**
	 * Prepare main list view with all controls
	 */
	private void prepareMainListView() {
		commandsList = (ListView) findViewById(R.id.command_list);
		commandsList.setEmptyView(findViewById(R.id.empty_list_view));
		commandsList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position > -1 && position < adapter.getCount()) {
					showItemDialogMenu(position);
				}
			}
		});
		adapter = new CommandListAdapter(app, app.getCommands());
		commandsList.setAdapter(adapter);
	}

	/**
	 * Reload adapter and commands list.
	 */
	public void reloadAdapter() {
		adapter.notifyDataSetChanged();
		commandsList.invalidateViews();
		commandsList.scrollBy(0, 0);
		commandsList.setFastScrollEnabled(app.getCommands().size() > 50);
		app.hideProgressDialog();
	}

	/**
	 * This method show the pop up menu when the user do a long click on a list
	 * item.
	 * 
	 * @param contactPosition
	 *            The contact position where was made the long click
	 */
	private void showItemDialogMenu(final int position) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.item_edit);
		builder.setItems(R.array.menu_list,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							onMenuItemSendSMS(position);
							break;
						case 1:
							onMenuItemEdit(position);
							break;
						case 2:
							onMenuItemAdd();
							break;
						case 3:
							onMenuItemDelete(position);
							break;
						}
					}
				});
		builder.create().show();
	}

	/**
	 * This method is invoked when the user chose to edit a command.
	 * 
	 * @param position
	 *            The position of command to be edited.
	 */
	private void onMenuItemEdit(int position) {
		Command command = (Command) adapter.getItem(position);
		new EditorDialog(this, R.string.edit_command, command).show();
	}

	/**
	 * This method is invoked when the user chose to add a new command.
	 */
	private void onMenuItemAdd() {
		new EditorDialog(this, R.string.add_command, null).show();
	}

	/**
	 * This method is invoked when the user chose to delete a command.
	 * 
	 * @param position
	 *            The position of command to be deleted.
	 */
	private void onMenuItemDelete(int position) {
		final Command command = (Command) adapter.getItem(position);
		if (command != null) {
			showConfirmationDialog(
					R.string.remove_command,
					app.getString(R.string.remove_command_question,
							command.getName()), CONFIRM_ID_DELETE, command);
		}
	}

	/**
	 * This method is invoked by the each time when is accepted a confirmation
	 * dialog.
	 * 
	 * @param positive
	 *            True if the confirmation is positive.
	 * @param confirmationId
	 *            The confirmation ID to identify the case.
	 * @param anObject
	 *            An object send by the caller method.
	 */
	@Override
	protected void onConfirmation(boolean positive, int confirmationId,
			Object anObject) {
		if (positive) {
			switch (confirmationId) {
			case CONFIRM_ID_DELETE:
				doDeleteCommand((Command) anObject);
				break;
			case CONFIRM_ID_SMS_SEND:
				doSendSMS((Command) anObject);
				break;
			case CONFIRM_ID_PARAMETERS:
				doPrepareSMSCommand((Command) anObject);
				break;
			case CONFIRM_ID_DONATE:
				startBrowserWithPage(R.string.donate_url);
				break;
			}
		} else {
			if (confirmationId == CONFIRM_ID_PARAMETERS) {
				showSendSMSConfirmation((Command) anObject);
			}
		}
	}

	/**
	 * Delete a command from the list.
	 * 
	 * @param command
	 *            The command to be deleted.
	 */
	private void doDeleteCommand(Command command) {
		app.showProgressDialog(this, R.string.please_wait);
		app.getCommands().remove(command);
		app.commandsSave();
		reloadAdapter();
	}

	/**
	 * This method is invoked when the user chose to send the command.
	 * 
	 * @param position
	 *            The position of command to be send.
	 */
	private void onMenuItemSendSMS(int position) {
		final Command command = (Command) adapter.getItem(position);
		app.prepareCommandParameters(command);
		if (command != null) {
			if (command.hasParameters()) {
				prepareSMSCommand(command);
			} else {
				showSendSMSConfirmation(command);
			}
		}
	}

	/**
	 * This method should be used to prepare the SMS command.
	 * 
	 * @param command
	 *            The SMS command to be prepared.
	 */
	private void prepareSMSCommand(Command command) {
		showConfirmationDialog(
				R.string.sms_prepare_title,
				app.getString(R.string.sms_prepare_message,
						command.getParametersListToBeShow()),
				CONFIRM_ID_PARAMETERS, command);
	}

	/**
	 * After the user is informed about the parameters he will be asked to edit
	 * each parameter.
	 * 
	 * @param command
	 *            The command to be prepared.
	 */
	private void doPrepareSMSCommand(Command command) {
		prepareCommandParameter(command, 0);
	}

	/**
	 * Method used to prepare parameters from the specified command.
	 * 
	 * @param command
	 *            The command to be prepared.
	 * @param parameterPosition
	 *            The parameter position to be prepared.
	 */
	public void prepareCommandParameter(final Command command,
			final int parameterPosition) {
		String temp = command.getParameterName(parameterPosition);
		if (Constants.PASSWORD.equals(temp)) {
			prepareCommandParameter(command, parameterPosition + 1);
		} else {
			if (parameterPosition < command.getParametersSize()) {
				ParameterEditor ped = new ParameterEditor(this, command,
						parameterPosition);
				ped.setOnDismissListener(new DialogInterface.OnDismissListener() {

					@Override
					public void onDismiss(DialogInterface dialog) {
						// move to next parameter
						prepareCommandParameter(command, parameterPosition + 1);
					}
				});
				ped.show();
			} else {
				if (command.hasParametersModified()) {
					app.saveCommandParameters(command);
					command.setParametersModified(false);
				}
				showSendSMSConfirmation(command);
			}
		}
	}

	/**
	 * Before to send the SMS a confirmation dialog is showed to the user to
	 * inform about the command.
	 * 
	 * @param command
	 *            The command to be send to the GPS tracker.
	 */
	private void showSendSMSConfirmation(final Command command) {
		int i, size = app.getContacts().size();
		CharSequence[] items = new CharSequence[size];
		boolean[] checkedItems = new boolean[size];
		i = 0;
		for (GpsContact contact : app.getContacts()) {
			items[i] = contact.getName();
			checkedItems[i] = contact.isSelected();
			i++;
		}
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_email)
				.setTitle(
						app.getString(R.string.send_sms_question,
								command.getSMSCommandShow()))
				.setMultiChoiceItems(items, checkedItems,
						new DialogInterface.OnMultiChoiceClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which, boolean isChecked) {
								GpsContact contact = app.getContacts().get(
										which);
								if (contact != null) {
									contact.setSelected(isChecked);
								}
							}
						})
				.setPositiveButton(R.string.yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								onConfirmation(true, CONFIRM_ID_SMS_SEND,
										command);
							}
						})
				.setNegativeButton(R.string.no,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								onConfirmation(false, CONFIRM_ID_SMS_SEND,
										command);
							}
						}).show();
	}

	/**
	 * Start the sending process of a SMS with the command to the GPS tracker.
	 * 
	 * @param command
	 *            The command to be send.
	 */
	private void doSendSMS(Command command) {
		String cmd = command.getSMSCommand();
		boolean result = false;
		for (GpsContact contact : app.getContacts()) {
			if (contact.isSelected()) {
				result = true;
				break;
			}
		}
		app.contactsSave();
		if (result) {
			app.sendSMS(this, TKConfigActivity.class, cmd);
		} else {
			showMessageDialog(R.string.information,
					app.getString(R.string.sms_no_contact), SMS_NO_CONTACT,
					command);
		}
	}

	/**
	 * This method is invoked when is selected a menu item from the option menu
	 * 
	 * @param menuItemId
	 *            The selected menu item
	 */
	@Override
	protected boolean onMenuItemSelected(int menuItemId) {
		boolean processed = false;
		switch (menuItemId) {
		case R.id.menu_add:
			processed = true;
			onMenuItemAdd();
			break;
		case R.id.menu_settings:
			processed = onMenuSettings();
			break;
		case R.id.menu_donate:
			processed = onMenuDonate();
			break;
		case R.id.menu_history:
			processed = onMenuHistory();
			break;
		case R.id.menu_about:
			processed = onMenuAbout();
			break;
		case R.id.menu_exit:
			processed = true;
			onExit();
			break;
		}
		return processed;
	}

	/**
	 * Show the about activity
	 */
	private boolean onMenuAbout() {
		Intent intent = new Intent(getBaseContext(), AboutActivity.class);
		startActivityForResult(intent, REQUEST_CODE_ABOUT);
		return true;
	}

	/**
	 * This is invoked when the user chose the donate item.
	 * 
	 * @return True, because this activity processed the menu item.
	 */
	private boolean onMenuDonate() {
		showConfirmationDialog(R.string.donate_title,
				app.getString(R.string.donate_message), CONFIRM_ID_DONATE, null);
		return true;
	}

	/**
	 * Show the settings activity (the preference activity)
	 * 
	 * @return True, because this activity processed the menu item.
	 */
	private boolean onMenuSettings() {
		Intent intent = new Intent(getBaseContext(), TkPreferences.class);
		startActivityForResult(intent, REQUEST_CODE_SETTINGS);
		return true;
	}

	/**
	 * Launch the default browser with a specified URL page.
	 * 
	 * @param urlResourceId
	 *            The URL resource id.
	 */
	private void startBrowserWithPage(int urlResourceId) {
		String url = app.getString(urlResourceId);
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	/**
	 * This method is invoked when a child activity is finished and this
	 * activity is showed again
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_SETTINGS) {
			if (app.isMustReloadCommands()) {
				app.showProgressDialog(this, R.string.please_wait);
				reloadAdapter();
				app.setMustReloadCommands(false);
			}
		}
	}

	/**
	 * Launch History Activity
	 * 
	 * @return True, because is processed by this activity.
	 */
	private boolean onMenuHistory() {
		Intent intent = new Intent(getBaseContext(), HistoryActivity.class);
		startActivityForResult(intent, 1);
		return true;
	}
}
