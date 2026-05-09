package com.hellochat.backend.repository;

import com.hellochat.backend.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {
}
