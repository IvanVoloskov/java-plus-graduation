package ewm.category.service;

import ewm.category.dto.CategoryDto;
import ewm.category.dto.NewCategoryDto;
import ewm.category.mapper.CategoryMapper;
import ewm.category.model.Category;
import ewm.category.repository.CategoryRepository;
import ewm.exception.ConflictException;
import ewm.exception.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        log.info("Добавление новой категории: {}", newCategoryDto.name());

        Category category = categoryMapper.toEntity(newCategoryDto);

        if (categoryRepository.existsByName(newCategoryDto.name())) {
            log.warn("Уже существует категория с именем: {}", newCategoryDto.name());
            throw new ConflictException("Category with name=" + newCategoryDto.name() + " already exists");
        }

        category = categoryRepository.save(category);
        log.info("Добавлена категория с ID: {}", category.getId());

        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional
    public void deleteCategoryById(Long categoryId) {
        log.info("Удаление категории с id: {}", categoryId);

        if (!categoryRepository.existsById(categoryId)) {
            throw new ConflictException("Category with id=" + categoryId + " was not found");
        }

        categoryRepository.deleteById(categoryId);
        log.info("Категория с id {} удалена", categoryId);
    }

    // имя категории должно быть уникальным
    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto categoryDto) {
        log.info("Обновление категории с id: {}, новое имя: {}", categoryId, categoryDto.name());

        Category category = existsCategory(categoryId);

        if (categoryRepository.existsByNameAndIdNot(categoryDto.name(), categoryId)) {
            log.warn("Уже существует категория с именем: {}", categoryDto.name());
            throw new ConflictException("Category with name=" + categoryDto.name() + " already exists");
        }

        category.setName(categoryDto.name());
        category = categoryRepository.save(category);
        log.info("Категория с id {} обновлена", categoryId);

        return categoryMapper.toDto(category);
    }

    @Override
    public List<CategoryDto> getAllCategory(Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());

        return categoryRepository.findAll(pageable).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        Category category = existsCategory(catId);

        log.info("Получена категория с id = {}", catId);

        return categoryMapper.toDto(category);
    }

    private Category existsCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + categoryId + " was not found"));
    }
}
