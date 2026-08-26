package com.example.productmanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.productmanager.dto.report.ReportResponseDTO;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.service.ReportService.ReportData;
import com.example.productmanager.service.ReportService.StatusStat;
import com.example.productmanager.service.ReportService.TopProductStat;

@Mapper(componentModel = "spring")
public interface ReportMapper {

	ReportResponseDTO toResponse(ReportData data);

	@Mapping(target = "status", expression = "java(toStatusValue(stat.status()))")
	ReportResponseDTO.StatusStatDTO toStatusStatDTO(StatusStat stat);

	ReportResponseDTO.TopProductStatDTO toTopProductStatDTO(TopProductStat stat);

	default String toStatusValue(OrderStatus status) {
		return status == null ? null : status.name();
	}
}
