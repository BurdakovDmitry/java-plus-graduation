package ru.practicum.compilation.mapper;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.mapper.EventMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(uses = EventMapper.class, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CompilationMapper {

    CompilationDto compilationToDto(Compilation compilation);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "pinned", defaultExpression  = "java(false)")
    @Mapping(target = "events", ignore = true)
    Compilation postDtoToCompilation(NewCompilationDto newCompilationDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", ignore = true)
    void updateDtoToCompilation(@MappingTarget Compilation compilation, UpdateCompilationDto dto);
}
