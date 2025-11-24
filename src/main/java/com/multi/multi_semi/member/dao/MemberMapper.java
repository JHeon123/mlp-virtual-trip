package com.multi.multi_semi.member.dao;


import com.multi.multi_semi.member.dto.MemberDto;
import com.multi.multi_semi.member.dto.req.MemberReqDto;
import com.multi.multi_semi.member.dto.res.MemberResDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface MemberMapper {

    Optional<MemberResDto> findMemberByNo(@Param("no") Long no);

    Optional<MemberResDto> findMemberById(@Param("id") String id);

    Optional<MemberResDto> findMemberByEmail(@Param("email") String email);

    Optional<MemberDto> findMemberForAuthByEmail(@Param("email") String email);

    int insertMember(MemberReqDto memberReqDto);

    int insertOAuthMember(MemberDto memberDto);

    int updateUuidByNo(@Param("no") Long no, @Param("uuid") String uuid);

    int updateMemberInfo(MemberDto memberDto);

    int deleteMemberByEmail(@Param("email") String email);
}
