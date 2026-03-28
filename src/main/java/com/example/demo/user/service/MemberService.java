package com.example.demo.user.service;

import com.example.demo.user.domain.Member;
import com.example.demo.user.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public Member upsert(String email, String name, String picture) {
        return memberRepository.findByEmail(email)
                .map(m -> {
                    m.update(name, picture);
                    return m;
                })
                .orElseGet(() -> memberRepository.save(Member.create(email, name, picture)));
    }
}
