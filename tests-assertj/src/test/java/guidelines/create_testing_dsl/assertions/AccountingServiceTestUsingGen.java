package guidelines.create_testing_dsl.assertions;

import static de.adesso.expressivetesting.system.domain.BookingType.Credit;
import static guidelines.create_testing_dsl.assertions.Assertions.assertThat;
import static guidelines.create_testing_dsl.assertions.CreditorConditions.name;
import static guidelines.create_testing_dsl.assertions.CreditorConditions.referenceId;
import static guidelines.create_testing_dsl.builder.domain.AccountBuilder.account;
import static guidelines.create_testing_dsl.builder.domain.AccountBuilder.anyAccount;
import static guidelines.create_testing_dsl.builder.domain.AccountBuilder.BalanceBookingSpecification.resultingFrom;
import static guidelines.create_testing_dsl.builder.domain.CreditorBuilder.anyCreditor;
import static guidelines.create_testing_dsl.builder.domain.CreditorBuilder.creditor;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import de.adesso.expressivetesting.system.domain.Account;
import de.adesso.expressivetesting.system.domain.Booking;
import de.adesso.expressivetesting.system.domain.CreditBooking;
import de.adesso.expressivetesting.system.service.AccountingService;
import testsupport.SpringComponentTest;

public class AccountingServiceTestUsingGen extends SpringComponentTest {

    @Autowired
    private AccountingService accountingService;
    
    private Date testStartDate;
    
    @Before
    public void setUp() {
        testStartDate = new Date();
    }
    
    @Test
    public void CreditNotExceedingBalance_MustAddCreditBookingToAccount() {
        Account account =
            account(anyAccount())
                .balance(EUR("100.00"), resultingFrom(1).booking())
            .build();
        
        Account resultingAccount = 
            accountingService
                .credit(
                    account, 
                    EUR("99.99"), 
                    creditor()
                        .name("Vattenfall Europe")
                        .referenceId("VE1234")
                        .build(), 
                    "Strom Abschlag"
                );
        
        assertThat(resultingAccount).describedAs("account").isNotNull();
        /*
         * NOTE: No assertion for checking the size of collections are generated by default, therefore we have to check
         *       it using the build-in assertions.
         *       It is possible to adjust the generator templates in order to also generate the necessary assertion
         */
        assertThat(resultingAccount.getBookings()).describedAs("account:bookings").hasSize(2);
        
        // NOTE: There is no (good) support for polymorhic types, therefore we have to split the assertions.
        assertThat(lastBookingIn(resultingAccount)).describedAs("lastBooking")
            .isNotNull()
            .isInstanceOf(CreditBooking.class);
        assertThat(lastBookingIn(resultingAccount, CreditBooking.class)).describedAs("lastBooking")
            .hasType(Credit)
            .hasAmount(EUR("99.99"))
            .hasReason("Strom Abschlag")
            .satisfies(
                // NOTE: No appropriate assertions for the 'date' and 'creditor' properties have been generated.
                //       Therefore, we must fall back to the build-in assertions.
                b -> {
                    assertThat(b.getDate()).as("lastBooking:date").isBetween(testStartDate, new Date(), true, true);
                    assertThat(b.getCreditor()).describedAs("lastBooking.creditor")
                        .isNotNull()
                        .has(allOf( // NOTE: We cannot use 'isEqualToComparingFieldByField' (wrong superclass)
                            name("Vattenfall Europe"), 
                            referenceId("VE1234")
                        ));
                }
            );
    }
    
    @Test
    public void CreditNotExceedingBalance_MustComputeAccountBalanceCorrectly() {
        Account account =
            account(anyAccount())
                .balance(EUR("100.00"), resultingFrom(3).bookings())
            .build();
        
        Account resultingAccount = 
            accountingService
                .credit(
                    account, 
                    EUR("99.99"), 
                    anyCreditor(), 
                    "Strom Abschlag"
                );
        
        assertThat(resultingAccount).describedAs("account")
            .isNotNull()
            .hasBalance(EUR("0.01"));
    }
    
    /*
     * NOTE: This is just syntactic sugar for specifying money amounts (assuming EUR).
     */
    private static BigDecimal EUR(String amount) {
        return new BigDecimal(amount);
    }
    
    /*
     * NOTE: This is just syntactic sugar for retrieving the last booking from a account.
     */
    private static Booking lastBookingIn(Account account) {
        List<Booking> bookings = account.getBookings();
        if (bookings.isEmpty()) {
            return null;
        }
        
        return bookings.get(bookings.size() - 1);
    }
    
    /*
     * NOTE: This is just syntactic sugar for retrieving the last booking from a account.
     */
    private static <T extends Booking> T lastBookingIn(Account account, Class<T> expectedType) {
        Booking booking = lastBookingIn(account);
        if (booking == null) {
            return null;
        }
        if (expectedType.isAssignableFrom(booking.getClass())) {
            return expectedType.cast(booking);
        }
        return null;
    }
}
