package com.hkb48.keepdo;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v7.widget.RecyclerView;

public class NavigationDrawerFragment extends Fragment {

    private static final int NAVDRAWER_ITEM_SORT = 1;
    private static final int NAVDRAWER_ITEM_IMPORT = 2;
    private static final int NAVDRAWER_ITEM_EXPORT = 3;
    private static final int NAVDRAWER_ITEM_BACKUP_RESTORE = 4;
    private static final int NAVDRAWER_ITEM_SETTINGS = 5;

    //Todo: to be declared in strings.xml
    private static final String[] NAVDRAWER_TITLE_RES_ID = {
            "Sort",
            "Export",
            "Import",
            "Backup&Restore(GoogleDrive)",
            "Settings"};

    private static final int[] NAVDRAWER_ICON_RES_ID = new int[]{
            R.drawable.ic_sort,
            R.drawable.ic_file_upload,
            R.drawable.ic_file_download,
            R.drawable.ic_drive,
            R.drawable.ic_settings
    };

    // Delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View mContainerView;

    public NavigationDrawerFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
    }

    public void setup(int fragmentId, DrawerLayout drawerLayout, Toolbar toolbar) {
        mContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;
        RecyclerView recyclerView = (RecyclerView) getActivity().findViewById(R.id.RecyclerView);
        recyclerView.setHasFixedSize(true);
        RecyclerView.Adapter adapter = new DrawerAdapter(NAVDRAWER_TITLE_RES_ID, NAVDRAWER_ICON_RES_ID);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        onNavDrawerItemClicked(position);
                    }
                })
        );

        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getActivity().invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                getActivity().invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });
    }

    public boolean isDrawerOpen() {
        return (mDrawerLayout != null) && (mContainerView != null) && mDrawerLayout.isDrawerOpen(mContainerView);
    }

    public void closeDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }
    }

    private void onNavDrawerItemClicked(final int itemId) {
        // launch the target Activity after a short delay, to allow the close animation to play
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                goToNavDrawerItem(itemId);
            }
        }, NAVDRAWER_LAUNCH_DELAY);
        closeDrawer();
    }

    private void goToNavDrawerItem(final int itemId) {
        Intent intent;
        switch (itemId) {
            case NAVDRAWER_ITEM_SORT:
                intent = new Intent(getActivity(), TaskSortingActivity.class);
                startActivity(intent);
                break;
            case NAVDRAWER_ITEM_IMPORT:
                break;
            case NAVDRAWER_ITEM_EXPORT:
                break;
            case NAVDRAWER_ITEM_BACKUP_RESTORE:
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    public class DrawerAdapter extends RecyclerView.Adapter<DrawerAdapter.ViewHolder> {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        private String mNavTitles[];
        private int mIcons[];

        public class ViewHolder extends RecyclerView.ViewHolder {
            int Holderid;

            TextView textView;
            ImageView imageView;

            public ViewHolder(View itemView, int ViewType) {
                super(itemView);

                if (ViewType == TYPE_ITEM) {
                    textView = (TextView) itemView.findViewById(R.id.title);
                    imageView = (ImageView) itemView.findViewById(R.id.icon);
                    Holderid = 1;
                } else {
                    Holderid = 0;
                }
            }
        }

        DrawerAdapter(String Titles[], int Icons[]) {
            mNavTitles = Titles;
            mIcons = Icons;
        }

        @Override
        public DrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_ITEM) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_item_row, parent, false);
                return new ViewHolder(v, viewType);
            } else if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.drawer_header, parent, false);
                return new ViewHolder(v, viewType);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(DrawerAdapter.ViewHolder holder, int position) {
            if (holder.Holderid == 1) {
                holder.textView.setText(mNavTitles[position - 1]);
                holder.imageView.setImageResource(mIcons[position - 1]);
            }
        }

        @Override
        public int getItemCount() {
            return mNavTitles.length + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (isPositionHeader(position))
                return TYPE_HEADER;

            return TYPE_ITEM;
        }

        private boolean isPositionHeader(int position) {
            return position == 0;
        }
    }
}
