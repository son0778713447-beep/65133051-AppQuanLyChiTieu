package nts.cntt2.quanlychitieu;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {
    private CalendarView calendarView;
    private RecyclerView rvCalendarTransactions;
    private TextView tvSelectedDateTitle;
    private TextView tvDayIncome, tvDayExpense, tvDayBalance;
    private TextView tvMonthDisplay;
    private ImageButton btnPrevMonth, btnNextMonth;
    private TransactionAdapter calendarAdapter;
    private TransactionViewModel transactionViewModel;
    private LocalDate selectedDate;
    private YearMonth currentMonth;
    private Set<LocalDate> transactionDates = new HashSet<>();

    private static class DayContainer extends ViewContainer {
        TextView tvDayNumber;
        TextView tvDayDot;
        CalendarDay day;

        DayContainer(@NonNull View view) {
            super(view);
            tvDayNumber = view.findViewById(R.id.tvDayNumber);
            tvDayDot = view.findViewById(R.id.tvDayDot);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        rvCalendarTransactions = view.findViewById(R.id.rvCalendarTransactions);
        tvSelectedDateTitle = view.findViewById(R.id.tvSelectedDateTitle);
        tvDayIncome = view.findViewById(R.id.tvDayIncome);
        tvDayExpense = view.findViewById(R.id.tvDayExpense);
        tvDayBalance = view.findViewById(R.id.tvDayBalance);
        tvMonthDisplay = view.findViewById(R.id.tvMonthDisplay);
        btnPrevMonth = view.findViewById(R.id.btnPrevMonth);
        btnNextMonth = view.findViewById(R.id.btnNextMonth);

        rvCalendarTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        calendarAdapter = new TransactionAdapter();
        rvCalendarTransactions.setAdapter(calendarAdapter);

        calendarAdapter.setOnDeleteClickListener(transaction -> {
            transactionViewModel.deleteTransaction(transaction);
        });

        transactionViewModel = new ViewModelProvider(requireActivity()).get(TransactionViewModel.class);

        selectedDate = LocalDate.now();
        currentMonth = YearMonth.now();
        updateMonthDisplay();

        calendarView.setup(
                currentMonth.minusMonths(12),
                currentMonth.plusMonths(12),
                DayOfWeek.MONDAY
        );
        calendarView.scrollToMonth(currentMonth);

        btnPrevMonth.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            updateMonthDisplay();
            calendarView.scrollToMonth(currentMonth);
        });

        btnNextMonth.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            updateMonthDisplay();
            calendarView.scrollToMonth(currentMonth);
        });

        calendarView.setDayBinder(new MonthDayBinder<DayContainer>() {
            @NonNull
            @Override
            public DayContainer create(@NonNull View view) {
                DayContainer container = new DayContainer(view);
                container.getView().setOnClickListener(v -> {
                    if (container.day != null && container.day.getPosition() == DayPosition.MonthDate) {
                        selectedDate = container.day.getDate();
                        calendarView.notifyCalendarChanged();
                        filterTransactionsByDate();
                    }
                });
                return container;
            }

            @Override
            public void bind(@NonNull DayContainer container, @NonNull CalendarDay data) {
                container.day = data;
                LocalDate date = data.getDate();

                container.tvDayNumber.setText(String.valueOf(date.getDayOfMonth()));

                if (data.getPosition() == DayPosition.MonthDate) {
                    container.tvDayNumber.setTextColor(0xFF212121);
                    if (transactionDates.contains(date)) {
                        container.tvDayDot.setVisibility(View.VISIBLE);
                    } else {
                        container.tvDayDot.setVisibility(View.GONE);
                    }

                    if (date.equals(selectedDate)) {
                        container.tvDayNumber.setBackgroundResource(R.drawable.circle_color_dot);
                        container.tvDayNumber.setTextColor(0xFFFFFFFF);
                    } else {
                        container.tvDayNumber.setBackground(null);
                    }
                } else {
                    container.tvDayNumber.setTextColor(0xFFBDBDBD);
                    container.tvDayDot.setVisibility(View.GONE);
                    container.tvDayNumber.setBackground(null);
                }
            }
        });

        transactionViewModel.getTransactionList().observe(getViewLifecycleOwner(), allTransactions -> {
            updateTransactionDates(allTransactions);
            calendarView.notifyCalendarChanged();
            filterTransactionsByDate();
        });

        return view;
    }

    private void updateTransactionDates(List<TransactionModel> allTransactions) {
        transactionDates.clear();
        if (allTransactions == null) return;
        for (TransactionModel trans : allTransactions) {
            if (trans.getTimestamp() != null) {
                Date date = trans.getTimestamp().toDate();
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                LocalDate localDate = LocalDate.of(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)
                );
                transactionDates.add(localDate);
            }
        }
    }

    private void filterTransactionsByDate() {
        String selectedDateStr = String.format(Locale.US, "%02d/%02d/%04d",
                selectedDate.getDayOfMonth(), selectedDate.getMonthValue(), selectedDate.getYear());

        if (tvSelectedDateTitle != null) {
            tvSelectedDateTitle.setText("Giao dịch ngày " + selectedDateStr);
        }

        List<TransactionModel> allTransactions = transactionViewModel.getTransactionList().getValue();
        if (allTransactions == null) return;

        List<TransactionModel> filteredList = new ArrayList<>();
        double totalIncome = 0, totalExpense = 0;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        for (TransactionModel trans : allTransactions) {
            if (trans.getTimestamp() != null) {
                String transDateStr = sdf.format(trans.getTimestamp().toDate());
                if (selectedDateStr.equals(transDateStr)) {
                    filteredList.add(trans);
                    if ("INCOME".equals(trans.getType())) {
                        totalIncome += trans.getAmount();
                    } else {
                        totalExpense += trans.getAmount();
                    }
                }
            }
        }
        calendarAdapter.setTransactions(filteredList);

        tvDayIncome.setText("+ " + String.format("%,.0fđ", totalIncome));
        tvDayExpense.setText("- " + String.format("%,.0fđ", totalExpense));
        double balance = totalIncome - totalExpense;
        String balanceSign = balance >= 0 ? "+ " : "";
        tvDayBalance.setText(balanceSign + String.format("%,.0fđ", balance));
    }

    private void updateMonthDisplay() {
        String[] monthNames = {"01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"};
        tvMonthDisplay.setText("Tháng " + monthNames[currentMonth.getMonthValue() - 1] + "/" + currentMonth.getYear());
    }
}
