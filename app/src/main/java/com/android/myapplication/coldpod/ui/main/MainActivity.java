package com.android.myapplication.coldpod.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.android.myapplication.coldpod.BaseApplication;
import com.android.myapplication.coldpod.R;
import com.android.myapplication.coldpod.ViewModelProviderFactory;
import com.android.myapplication.coldpod.network.data.Enclosure;
import com.android.myapplication.coldpod.network.data.ItemImage;
import com.android.myapplication.coldpod.persistence.FavoriteEntry;
import com.android.myapplication.coldpod.persistence.Item;
import com.android.myapplication.coldpod.persistence.PodcastEntry;
import com.android.myapplication.coldpod.databinding.ActivityMainBinding;
import com.android.myapplication.coldpod.service.PodcastService;
import com.android.myapplication.coldpod.ui.main.favorites.FavoritesFragment;
import com.android.myapplication.coldpod.ui.main.subscribed.SubscribedFragment;
import com.android.myapplication.coldpod.ui.playing.PlayingActivity;
import com.android.myapplication.coldpod.ui.podcast_entry.PodCastEntryActivity;
import com.android.myapplication.coldpod.ui.podcasts.PodCastListActivity;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;


public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        SubscribedFragment.Listener, FavoritesFragment.Listener {


    ActivityMainBinding mBinding;
    DrawerLayout drawerLayout;
    Toolbar mToolbar;
    SpannableString mSpannableString;
    ForegroundColorSpan mForegroundColorSpan;
    NavigationView mNavigationView;
    MainActivityViewModel mViewModel;


    @Inject
    ViewModelProviderFactory providerFactory;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpDagger();
        initializeMemberVariables();
        setupSpannableTitle();
        setupToolbarAndNavDrawer(savedInstanceState);
    }

    private void setUpDagger() {
        ((BaseApplication) getApplication()).getAppComponent().getMainComponent()
                .inject(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        subscribeObserver();
    }

    private void subscribeObserver() {
        mViewModel.getNavToPodcastsActivity().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                if (aBoolean) {
                    startActivity(PodCastListActivity.getInstance(MainActivity.this));
                    // overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                    //reseting
                    mViewModel.setNavToPodcastsActivity(false);
                }
            }
        });
    }

    private void initializeMemberVariables() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mBinding.setLifecycleOwner(this);
        drawerLayout = mBinding.drawerLayout;
        mToolbar = mBinding.toolbar;
        mNavigationView = mBinding.navView;
        mSpannableString = new SpannableString(getString(R.string.app_name));
        mForegroundColorSpan = new ForegroundColorSpan(ContextCompat.getColor(this, (R.color.primary_color)));
        mViewModel = new ViewModelProvider(this, providerFactory).get(MainActivityViewModel.class);
        mBinding.setViewModel(mViewModel);
    }

    private void setupSpannableTitle() {
        mSpannableString.setSpan(mForegroundColorSpan, 4, 7, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
    }

    private void setupToolbarAndNavDrawer(Bundle savedInstanceState) {
        this.setSupportActionBar(mToolbar);
        ((AppCompatActivity) this).getSupportActionBar().setTitle(mSpannableString);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        mNavigationView.setNavigationItemSelectedListener(this);
        if (savedInstanceState == null) {
            performTransaction(new SubscribedFragment());
            mNavigationView.setCheckedItem(R.id.drawer_nav_podcasts);
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.drawer_nav_podcasts:
                performTransaction(new SubscribedFragment());
                break;
            case R.id.drawer_nav_favorites:
                performTransaction(new FavoritesFragment());
                break;
            default:
                performTransaction(new SubscribedFragment());
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void performTransaction(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragments_container, fragment).commit();
        if (fragment instanceof SubscribedFragment) {
            mViewModel.setIfFabVisible(true);
        } else {
            mViewModel.setIfFabVisible(false);
        }
    }


    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().findFragmentById(R.id.fragments_container) instanceof SubscribedFragment) {
            mViewModel.setIfFabVisible(true);
        }
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onPodCastEntryClicked(PodcastEntry podcastEntry) {
        Intent intent = PodCastEntryActivity.getInstance(this, podcastEntry.getPodcastId(), podcastEntry.getTitle());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(FavoriteEntry favoriteEntry) {
        Intent serviceIntent = PodcastService.getInstance(this, getItem(favoriteEntry), favoriteEntry.getArtworkImageUrl(), favoriteEntry.getTitle(), favoriteEntry.getPodcastId());
        startService(serviceIntent);
        Intent playingIntent = PlayingActivity.getInstance(this, getItem(favoriteEntry), favoriteEntry.getPodcastId(), favoriteEntry.getArtworkImageUrl(), favoriteEntry.getTitle());
        startActivity(playingIntent);
    }

    //creating the dependency of PodCastService and PlayingActivity from the FavoriteEntry
    private Item getItem(FavoriteEntry favoriteEntry) {
        String itemTitle = favoriteEntry.getItemTitle();
        String itemDescription = favoriteEntry.getItemDescription();
        String iTunesSummary = favoriteEntry.getItemDescription();
        String pubDate = favoriteEntry.getItemPubDate();
        String duration = favoriteEntry.getItemDuration();

        String enclosureUrl = favoriteEntry.getItemEnclosureUrl();
        String enclosureType = favoriteEntry.getItemEnclosureType();
        String enclosureLength = favoriteEntry.getItemEnclosureLength();
        List<Enclosure> enclosures = new ArrayList<>();
        Enclosure enclosure = new Enclosure(enclosureUrl, enclosureType, enclosureLength);
        enclosures.add(enclosure);
        String itemImageUrl = favoriteEntry.getItemImageUrl();
        ItemImage itemImage = new ItemImage(itemImageUrl);
        List<ItemImage> itemImages = new ArrayList<>();
        itemImages.add(itemImage);

        return new Item(itemTitle, itemDescription, iTunesSummary, pubDate, duration, enclosures, itemImages);
    }
}

