package com.example.demo.auth.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VerifyCodeStoreTest {

    private VerifyCodeStore store;

    @BeforeEach
    void setUp() {
        store = new VerifyCodeStore();
    }

    @Test
    @DisplayName("코드를 저장하고 조회할 수 있다")
    void saveAndFind() {
        store.save("user@kr.doubledown.com", "123456");

        Optional<String> result = store.find("user@kr.doubledown.com");

        assertThat(result).isPresent().contains("123456");
    }

    @Test
    @DisplayName("저장되지 않은 이메일은 빈 결과를 반환한다")
    void findNonExistent() {
        Optional<String> result = store.find("unknown@kr.doubledown.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("코드를 삭제하면 더 이상 조회되지 않는다")
    void remove() {
        store.save("user@kr.doubledown.com", "123456");
        store.remove("user@kr.doubledown.com");

        Optional<String> result = store.find("user@kr.doubledown.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("같은 이메일로 재저장하면 이전 코드가 덮어씌워진다")
    void overwrite() {
        store.save("user@kr.doubledown.com", "111111");
        store.save("user@kr.doubledown.com", "222222");

        Optional<String> result = store.find("user@kr.doubledown.com");

        assertThat(result).isPresent().contains("222222");
    }

    @Test
    @DisplayName("실패 5회 시 잠금 상태가 된다")
    void lockAfterFiveFailures() {
        String email = "user@kr.doubledown.com";
        store.save(email, "123456");

        for (int i = 0; i < 5; i++) {
            store.recordFailure(email);
        }

        assertThat(store.isLocked(email)).isTrue();
    }

    @Test
    @DisplayName("실패 4회까지는 잠금되지 않는다")
    void notLockedBeforeFiveFailures() {
        String email = "user@kr.doubledown.com";

        for (int i = 0; i < 4; i++) {
            store.recordFailure(email);
        }

        assertThat(store.isLocked(email)).isFalse();
    }

    @Test
    @DisplayName("잠금 시 저장된 코드도 삭제된다")
    void lockRemovesCode() {
        String email = "user@kr.doubledown.com";
        store.save(email, "123456");

        for (int i = 0; i < 5; i++) {
            store.recordFailure(email);
        }

        assertThat(store.find(email)).isEmpty();
    }

    @Test
    @DisplayName("코드 재저장 시 실패 횟수가 초기화된다")
    void resaveResetsFailCount() {
        String email = "user@kr.doubledown.com";

        for (int i = 0; i < 4; i++) {
            store.recordFailure(email);
        }
        assertThat(store.getFailCount(email)).isEqualTo(4);

        store.save(email, "999999");
        assertThat(store.getFailCount(email)).isZero();
    }
}
