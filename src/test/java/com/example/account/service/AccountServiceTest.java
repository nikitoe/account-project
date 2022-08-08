package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountUser;
import com.example.account.dto.AccountDto;
import com.example.account.exception.AccountException;
import com.example.account.repository.AccountUserRepository;
import com.example.account.type.AccountStatus;
import com.example.account.repository.AccountRepository;
import com.example.account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성_순차 증가 방식- 성공 응답")
    void createAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1000000012").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000013", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성_계좌 최초 생성시 10자리의 정수(1000000000)")
    void createFirstAccount() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findFirstByOrderByIdDesc())
                .willReturn(Optional.empty());
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(user)
                        .accountNumber("1000000015").build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.createAccount(1L, 1000L);

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(15L, accountDto.getUserId());
        assertEquals("1000000000", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성_해당 유저 없음 - 실패 응답")
    void createAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());


        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 생성_유저 당 최대 계좌는 10개 - 실패 응답")
    void createAccount_maxAccountIs10() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(15L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);
        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        //then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 - 성공 응답")
    void deleteAccountSuccess() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(user)
                        .balance(0L)
                        .accountNumber("1000000012").build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        //when
        AccountDto accountDto = accountService.deleteAccount(1L, "1234567890");

        //then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(12L, accountDto.getUserId());
        assertEquals("1000000012", captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("계좌 해지_사용자 없는 경우 - 실패 응답")
    void deleteAccount_UserNotFound() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지_해당 계좌가 없는 경우 - 실패 응답")
    void deleteAccount_AccountNotFound() {
        //given
        AccountUser user = AccountUser.builder()
                .name("Pobi").build();
        user.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(user));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지_사용자 아이디와 계좌 소유주가 다른 경우 - 실패 응답")
    void deleteAccountFailed_userUnMatch() {
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        AccountUser potter = AccountUser.builder()
                .name("Potter").build();
        potter.setId(13L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(potter)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 해지_잔액이 있는 경우 - 실패 응답")
    void deleteAccountFailed_balanceNotEmpty() {
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .balance(100L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.BALANCE_NOT_EMPTY, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 해지_계좌가 이미 해지 상태인 경우 - 실패 응답")
    void deleteAccountFailed_alreadyUnRegistered() {
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(pobi)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .balance(0L)
                        .accountNumber("1000000012").build()));

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        //then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 조회 - 성공 응답")
    void successGetAccountsByUserId() {
        //given
        AccountUser pobi = AccountUser.builder()
                .name("Pobi").build();
        pobi.setId(12L);
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("1111111111")
                        .balance(100L)
                        .build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("2222222222")
                        .balance(200L)
                        .build(),
                Account.builder()
                        .accountUser(pobi)
                        .accountNumber("3333333333")
                        .balance(300L)
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(pobi));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(1L);

        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals(100, accountDtos.get(0).getBalance());
        assertEquals("2222222222", accountDtos.get(1).getAccountNumber());
        assertEquals(200, accountDtos.get(1).getBalance());
        assertEquals("3333333333", accountDtos.get(2).getAccountNumber());
        assertEquals(300, accountDtos.get(2).getBalance());

    }

    @Test
    @DisplayName("계좌 조회_사용자 없는 경우 - 실패 응답")
    void failedToGetAccounts() {
        //given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        //when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        //then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}