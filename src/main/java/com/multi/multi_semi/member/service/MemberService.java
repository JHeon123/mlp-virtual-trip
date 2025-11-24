package com.multi.multi_semi.member.service;


import com.multi.multi_semi.common.exception.MemberNotFoundException;
import com.multi.multi_semi.member.dao.MemberMapper;
import com.multi.multi_semi.member.dto.MemberDto;
import com.multi.multi_semi.member.dto.req.MemberReqDto;
import com.multi.multi_semi.member.dto.res.MemberResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberResDto findMemberByEmail(String email) {
        Optional<MemberResDto> memberResDto = memberMapper.findMemberByEmail(email);

        if(memberResDto.isEmpty()){
            throw new MemberNotFoundException("회원정보를 찾을 수 없습니다");
        }

        return memberResDto.get();
    }

    public MemberResDto findMemberByNo(Long no) {
        Optional<MemberResDto> memberResDto = memberMapper.findMemberByNo(no);

        if(memberResDto.isEmpty()){
            throw new MemberNotFoundException("회원정보를 찾을 수 없습니다");
        }

        return memberResDto.get();
    }

    public void updateMemberInfo(String email, MemberReqDto updateRequest) {
        MemberDto memberDto = new MemberDto();
        memberDto.setEmail(email);

        memberDto.setId(updateRequest.getId());
        memberDto.setName(updateRequest.getName());
        memberDto.setAddr(updateRequest.getAddr());
        memberDto.setPhone(updateRequest.getPhone());
        memberDto.setIntro(updateRequest.getIntro());

        // 클라이언트가 비밀번호를 입력하였으면 수정. 입력하지 않았으면 기존 비밀번호 유지
        String newPassword = updateRequest.getPwd();
        if (newPassword != null && !newPassword.isEmpty()) {
            memberDto.setPwd(passwordEncoder.encode(newPassword));
        }

        memberMapper.updateMemberInfo(memberDto);
    }

    public int deleteMemberByEmail(String email) {
        int result = memberMapper.deleteMemberByEmail(email);
        return result;
    }
}
