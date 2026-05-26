package nts.cntt2.quanlychitieu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarFragment extends Fragment {
    private CalendarView calendarView;
    private RecyclerView rvCalendarTransactions;
    private TextView tvSelectedDateTitle;
    private TransactionAdapter calendarAdapter;
    private TransactionViewModel transactionViewModel;
    private long selectedDateMillis;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        rvCalendarTransactions = view.findViewById(R.id.rvCalendarTransactions);
        tvSelectedDateTitle = view.findViewById(R.id.tvSelectedDateTitle);

        rvCalendarTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        calendarAdapter = new TransactionAdapter();
        rvCalendarTransactions.setAdapter(calendarAdapter);

        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        selectedDateMillis = calendarView.getDate();

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDateMillis = calendar.getTimeInMillis();
            filterTransactionsByDate();
        });

        transactionViewModel.getTransactionList().observe(getViewLifecycleOwner(), allTransactions -> {
            filterTransactionsByDate();
        });

        return view;
    }

    private void filterTransactionsByDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String selectedDateStr = sdf.format(new Date(selectedDateMillis));

        if (tvSelectedDateTitle != null) {
            tvSelectedDateTitle.setText("Giao dịch ngày " + selectedDateStr);
        }

        List<TransactionModel> allTransactions = transactionViewModel.getTransactionList().getValue();
        if (allTransactions == null) return;

        List<TransactionModel> filteredList = new ArrayList<>();
        for (TransactionModel trans : allTransactions) {
            if (trans.getTimestamp() != null) {
                String transDateStr = sdf.format(trans.getTimestamp().toDate());
                if (selectedDateStr.equals(transDateStr)) {
                    filteredList.add(trans);
                }
            }
        }
        calendarAdapter.setTransactions(filteredList);
    }
}
