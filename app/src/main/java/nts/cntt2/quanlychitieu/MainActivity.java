package nts.cntt2.quanlychitieu;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;
    private TransactionViewModel transactionViewModel;

    private WalletFragment walletFragment;
    private AddFragment addFragment;
    private CalendarFragment calendarFragment;
    private ReportFragment reportFragment;

    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        transactionViewModel = new ViewModelProvider(this).get(TransactionViewModel.class);
        transactionViewModel.listenToTransactions();

        walletFragment = new WalletFragment();
        addFragment = new AddFragment();
        calendarFragment = new CalendarFragment();
        reportFragment = new ReportFragment();

        addFragment.setOnTransactionSavedListener(() -> {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            showFragment(0);
        });

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, walletFragment, "wallet")
                .add(R.id.fragmentContainer, addFragment, "add")
                .add(R.id.fragmentContainer, calendarFragment, "calendar")
                .add(R.id.fragmentContainer, reportFragment, "report")
                .hide(addFragment)
                .hide(calendarFragment)
                .hide(reportFragment)
                .commit();

        if (getIntent().hasExtra("open_calendar")) {
            showFragment(2);
            bottomNavigation.setSelectedItemId(R.id.nav_calendar);
            getIntent().removeExtra("open_calendar");
        } else if (getIntent().hasExtra("open_report")) {
            showFragment(3);
            bottomNavigation.setSelectedItemId(R.id.nav_report);
            getIntent().removeExtra("open_report");
        } else if (getIntent().hasExtra("open_add")) {
            showFragment(1);
            bottomNavigation.setSelectedItemId(R.id.nav_add);
            getIntent().removeExtra("open_add");
        } else {
            showFragment(0);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                showFragment(0);
                return true;
            } else if (id == R.id.nav_add) {
                showFragment(1);
                return true;
            } else if (id == R.id.nav_calendar) {
                showFragment(2);
                return true;
            } else if (id == R.id.nav_report) {
                showFragment(3);
                return true;
            }
            return false;
        });
    }

    private void showFragment(int index) {
        if (currentTab == index) return;

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        switch (currentTab) {
            case 0: ft.hide(walletFragment); break;
            case 1: ft.hide(addFragment); break;
            case 2: ft.hide(calendarFragment); break;
            case 3: ft.hide(reportFragment); break;
        }

        switch (index) {
            case 0: ft.show(walletFragment); break;
            case 1: ft.show(addFragment); break;
            case 2: ft.show(calendarFragment); break;
            case 3: ft.show(reportFragment); break;
        }

        currentTab = index;
        ft.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        transactionViewModel.forceRefresh();
    }
}
