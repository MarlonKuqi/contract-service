package com.mk.contractservice.features.contract.importjob;

import com.mk.contractservice.domain.contract.importjob.ContractImportJob;
import com.mk.contractservice.domain.contract.importjob.ContractImportJobRepository;
import com.mk.contractservice.features.shared.InvalidPageRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListContractImportJobs Handler")
class ListContractImportJobsTest {

    @Mock
    private ContractImportJobRepository jobRepository;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    @Test
    @DisplayName("GIVEN a counselor filter WHEN listing THEN only its jobs are read, newest first")
    void shouldListJobsOfCounselorNewestFirst() {
        final Page<ContractImportJob> page = Page.empty();
        when(jobRepository.findAll(eq("marie.counselor"), any())).thenReturn(page);

        final Page<ContractImportJob> result = new ListContractImportJobs.Handler(jobRepository)
                .execute(new ListContractImportJobs.Query("marie.counselor", 2, 10));

        assertThat(result).isSameAs(page);
        verify(jobRepository).findAll(eq("marie.counselor"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    @Test
    @DisplayName("GIVEN no filter WHEN listing THEN the jobs of every counselor are read")
    void shouldListEveryJobWithoutFilter() {
        when(jobRepository.findAll(isNull(), any())).thenReturn(Page.empty());

        new ListContractImportJobs.Handler(jobRepository).execute(new ListContractImportJobs.Query(null, 0, 20));

        verify(jobRepository).findAll(isNull(), any());
    }

    @Test
    @DisplayName("GIVEN page bounds WHEN building Query THEN they follow the listing limits")
    void shouldCheckPageBounds() {
        assertThatCode(() -> new ListContractImportJobs.Query(null, 0, ListContractImportJobs.MAX_PAGE_SIZE))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new ListContractImportJobs.Query(null, -1, 20))
                .isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> new ListContractImportJobs.Query(null, 0, ListContractImportJobs.MAX_PAGE_SIZE + 1))
                .isInstanceOf(InvalidPageRequestException.class);
    }
}
