package com.server.popfilterbubbleserver.repository;

import com.server.popfilterbubbleserver.module.YoutubeChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YoutubeRepository extends JpaRepository<YoutubeChannelEntity, String> {
}