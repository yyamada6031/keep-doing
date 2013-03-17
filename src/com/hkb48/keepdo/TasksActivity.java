package com.hkb48.keepdo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class TasksActivity extends Activity {
    // Request code when launching sub-activity
    private static final int REQUEST_ADD_TASK = 0;
    private static final int REQUEST_EDIT_TASK = 1;
    private static final int REQUEST_SHOW_CALENDAR = 2;

    // ID of context menu items
    private static final int CONTEXT_MENU_EDIT = 0;
    private static final int CONTEXT_MENU_DELETE = 1;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private TaskAdapter mAdapter;
    private List<TaskListItem> mDataList = new ArrayList<TaskListItem>();
    private CheckSoundPlayer mCheckSound = new CheckSoundPlayer(this);
    private DatabaseAdapter mDBAdapter = null;
    private int mDoneIconId = 0;
    private int mNotDoneIconId = 0;

    private Settings.OnSettingsChangeListener mListener = new Settings.OnSettingsChangeListener() {
        public void onSettingsChanged() {
            mDoneIconId = Settings.getDoneIconId();
            mNotDoneIconId = Settings.getNotDoneIconId();
            updateTaskList();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDBAdapter = DatabaseAdapter.getInstance(this);

        Settings.initialize(getApplicationContext());
        Settings.setOnPreferenceChangeListener(mListener);
        mDoneIconId = Settings.getDoneIconId();
        mNotDoneIconId = Settings.getNotDoneIconId();

        // Cancel notification (if displayed)
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(R.string.app_name);

        ListView taskListView = (ListView) findViewById(R.id.mainListView);
        mAdapter = new TaskAdapter();
        taskListView.setAdapter(mAdapter);

        taskListView.setEmptyView(findViewById(R.id.empty));

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // Show calendar view
                Task task = (Task) mDataList.get(position).data;
                Long taskId = task.getTaskID();
                Intent intent = new Intent(TasksActivity.this, TaskActivity.class);
                intent.putExtra("TASK-ID", taskId);
                startActivityForResult(intent, REQUEST_SHOW_CALENDAR);
            }
        });

        registerForContextMenu(taskListView);

        updateTaskList();
    }

    @Override
    public void onResume() {
        mCheckSound.load();
        super.onResume();
    }

    @Override
    public void onPause() {
        mCheckSound.unload();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mDBAdapter.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.menu_add_task:
            intent = new Intent(TasksActivity.this, TaskSettingActivity.class);
            startActivityForResult(intent, REQUEST_ADD_TASK);
            return true;
        case R.id.menu_settings:
            intent = new Intent(TasksActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        case R.id.menu_backup_restore:
        	// Show a backup & restore dialog
        	AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(TasksActivity.this);
        	dialogBuilder.setTitle(R.string.backup_restore);
        	dialogBuilder.setSingleChoiceItems(R.array.dialog_choice_backup_restore, -1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            });
        	dialogBuilder.setNegativeButton(R.string.dialog_cancel, null);
        	dialogBuilder.setPositiveButton(R.string.dialog_start, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                	switch (((AlertDialog)dialog).getListView().getCheckedItemPosition()) {
                	case 0:
                		// execute backup
                		backupTaskData();
                		Toast.makeText(TasksActivity.this, R.string.backup_done, Toast.LENGTH_SHORT).show();
                		return;
                	case 1:
                		// execute restore
                		restoreTaskData();
                        updateTaskList();
                		Toast.makeText(TasksActivity.this, R.string.restore_done, Toast.LENGTH_SHORT).show();
                		return;
                	default:
                		return;
                	}
                }
            });
        	dialogBuilder.setCancelable(true);
        	dialogBuilder.show().getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Task task;
            switch(requestCode) {
            case REQUEST_ADD_TASK:
                task = (Task) data.getSerializableExtra("TASK-INFO");
                mDBAdapter.addTask(task);
                updateTaskList();
                updateReminder();
                break;
            case REQUEST_EDIT_TASK:
                task = (Task) data.getSerializableExtra("TASK-INFO");
                mDBAdapter.editTask(task);
                updateTaskList();
                updateReminder();
                break;
            case REQUEST_SHOW_CALENDAR:
                updateTaskList();
                break;
            default:
                break;
            }
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterContextMenuInfo adapterinfo = (AdapterContextMenuInfo) menuInfo;
        ListView listView = (ListView) view;
        TaskListItem taskListItem = (TaskListItem) listView.getItemAtPosition(adapterinfo.position);
        Task task = (Task) taskListItem.data;
        menu.setHeaderTitle(task.getName());
        menu.add(0, CONTEXT_MENU_EDIT, 0, R.string.edit_task);
        menu.add(0, CONTEXT_MENU_DELETE, 1, R.string.delete_task);
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        TaskListItem taskListItem = (TaskListItem) mAdapter.getItem(info.position);
        Task task = (Task) taskListItem.data;
        final long taskId = task.getTaskID();
        switch (item.getItemId()) {
        case CONTEXT_MENU_EDIT:
            Intent intent = new Intent(TasksActivity.this, TaskSettingActivity.class);
            intent.putExtra("TASK-INFO", task);
            startActivityForResult(intent, REQUEST_EDIT_TASK);
            return true;
        case CONTEXT_MENU_DELETE:
            new AlertDialog.Builder(this)
            .setMessage(R.string.delete_confirmation)
            .setPositiveButton(R.string.dialog_ok ,new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mDBAdapter.deleteTask(taskId);
                    updateTaskList();
                    updateReminder();
                }
            })
            .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            })
            .setCancelable(true)
            .create()
            .show();
            return true;
        default:
            Toast.makeText(this, "default", Toast.LENGTH_SHORT).show();
            return super.onContextItemSelected(item);
        }
    }

    /**
     * Update the task list view with latest DB information.
     */
    private void updateTaskList() {
        List<Task> taskList = mDBAdapter.getTaskList();

        List<Task> taskListToday =  new ArrayList<Task>();
        List<Task> taskListNotToday = new ArrayList<Task>();
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);

        mDataList.clear();
        for (Task task : taskList) {
            if (task.getRecurrence().isValidDay(dayOfWeek)) {
                taskListToday.add(task);
            } else {
                taskListNotToday.add(task);
            }
        }

        if (taskListToday.size() > 0) {
            // Dummy Task for header on the ListView
            TaskListHeader header = new TaskListHeader();
            header.title = getString(R.string.tasklist_header_today_task);
            SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.date_format));
            Date today = DateChangeTime.getDate();
            header.subText = sdf.format(today);
            TaskListItem taskListItem = new TaskListItem(TYPE_HEADER, header);
            mDataList.add(taskListItem);

            for (Task task : taskListToday) {
                taskListItem = new TaskListItem(TYPE_ITEM, task);
                mDataList.add(taskListItem);
            }
        }
        if (taskListNotToday.size() > 0) {
            // Dummy Task for header on the ListView
            TaskListHeader header = new TaskListHeader();
            header.title = getString(R.string.tasklist_header_other_task);
            TaskListItem taskListItem = new TaskListItem(TYPE_HEADER, header);
            mDataList.add(taskListItem);

            for (Task task : taskListNotToday) {
                taskListItem = new TaskListItem(TYPE_ITEM, task);
                mDataList.add(taskListItem);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private void updateReminder() {
        ReminderManager.getInstance().setNextAlert(this);
    }

    /**
     * Backup & Restore
     */
    private void backupTaskData() {
    	DatabaseHelper dbHelper = new DatabaseHelper(this);
    	dbHelper.backupDataBase(Environment.getExternalStorageDirectory().getPath() + "/keepdo", "/keepdo.db" );
    }
    private void restoreTaskData() {
    	DatabaseHelper dbHelper = new DatabaseHelper(this);
    	dbHelper.restoreDataBase(Environment.getExternalStorageDirectory().getPath() + "/keepdo/keepdo.db" );
    }

    /**
     * 
     */
    private static class TaskListItem {
        int type;
        Object data;

        public TaskListItem(int type, Object data) {
            this.type = type;
            this.data = data;
        }
    }

    private static class TaskListHeader {
        String title;
        String subText;
    }

    private class TaskAdapter extends BaseAdapter {
        public int getCount() {
            return mDataList.size();
        }

        public Object getItem(int position) {
            return mDataList.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public int getItemViewType(int position) {
            TaskListItem item = (TaskListItem) getItem(position);
            return item.type;
        }

        public boolean isEnabled(int position) {
            return (getItemViewType(position) == TYPE_ITEM);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = convertView;
            TaskListItem taskListItem = (TaskListItem) getItem(position);
            boolean isTask = isEnabled(position);
            boolean createView = false;
            HeaderViewHolder headerViewHolder = null;
            ItemViewHolder itemViewHolder = null;

            // Check if it's necessary to create view or re-use
            if (view == null) {
                createView = true;
            } else {
                ViewHolder viewHolder = (ViewHolder) view.getTag();
                if (viewHolder.viewType != getItemViewType(position)) {
                    createView = true;
                } else {
                    if (isTask) {
                        itemViewHolder = (ItemViewHolder) viewHolder;
                    } else {
                        headerViewHolder = (HeaderViewHolder) viewHolder;
                    }
                }
            }

            if (createView) {
                if (isTask) {
                    itemViewHolder = new ItemViewHolder();
                    view = inflater.inflate(R.layout.task_list_row, null);
                    itemViewHolder.viewType = TYPE_ITEM;
                    itemViewHolder.imageView = (ImageView) view.findViewById(R.id.taskListItemCheck);
                    itemViewHolder.textView1 = (TextView) view.findViewById(R.id.taskName);
                    itemViewHolder.recurrenceView = (RecurrenceView) view.findViewById(R.id.recurrenceView);
                    view.setTag(itemViewHolder);
                } else {
                    headerViewHolder = new HeaderViewHolder();
                    view = inflater.inflate(R.layout.task_list_header, null);
                    headerViewHolder.viewType = TYPE_HEADER;
                    headerViewHolder.textView1 = (TextView) view.findViewById(R.id.textView1);
                    headerViewHolder.textView2 = (TextView) view.findViewById(R.id.textView2);
                    view.setTag(headerViewHolder);
                }
            }

            if (isTask) {
                TextView textView = itemViewHolder.textView1;
                Task task = (Task) taskListItem.data;
                String taskName = task.getName();
                textView.setText(taskName);

                RecurrenceView recurrenceView = itemViewHolder.recurrenceView;
                recurrenceView.setTextSize(12.0f);
                recurrenceView.update(task.getRecurrence());

                ImageView imageView = itemViewHolder.imageView;
                Date today = DateChangeTime.getDate();
                boolean checked = mDBAdapter.getDoneStatus(task.getTaskID(), today);

                if (checked) {
                    imageView.setImageResource(mDoneIconId);
                } else {
                    imageView.setImageResource(mNotDoneIconId);
                }
                imageView.setTag(Integer.valueOf(position));
                imageView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ImageView imageView = (ImageView) v;
                        int position = (Integer) v.getTag();
                        TaskListItem taskListItem = (TaskListItem) getItem(position);
                        Task task = (Task) taskListItem.data;
                        long taskId = task.getTaskID();
                        Date today = DateChangeTime.getDate();
                        boolean checked = mDBAdapter.getDoneStatus(taskId, today);
                        checked = ! checked;
                        mDBAdapter.setDoneStatus(taskId, today, checked);
                        updateReminder();

                        if (checked) {
                            imageView.setImageResource(mDoneIconId);
                            mCheckSound.play();
                        } else {
                            imageView.setImageResource(mNotDoneIconId);
                        }
                    }
                });
            } else {
                TaskListHeader taskListHeader = (TaskListHeader) taskListItem.data;
                headerViewHolder.textView1.setText(taskListHeader.title);
                headerViewHolder.textView2.setText(taskListHeader.subText);
            }

            return view;
        }

        private class ViewHolder {
            int viewType;
        }

        private class HeaderViewHolder extends ViewHolder {
            TextView textView1;
            TextView textView2;
        }

        private class ItemViewHolder extends ViewHolder {
            TextView textView1;
            ImageView imageView;
            RecurrenceView recurrenceView;
        }
    }
}
