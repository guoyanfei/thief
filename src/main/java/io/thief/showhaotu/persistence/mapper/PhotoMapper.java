package io.thief.showhaotu.persistence.mapper;

import io.thief.showhaotu.persistence.model.Photo;

import java.util.List;

public interface PhotoMapper {

	public void insert(Photo photo);

	public Photo find(Long id);

	public void consume(Long id);

	public List<Photo> findAllUnDownload();
}
