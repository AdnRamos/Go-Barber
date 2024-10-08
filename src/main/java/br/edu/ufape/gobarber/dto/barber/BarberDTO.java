package br.edu.ufape.gobarber.dto.barber;

import br.edu.ufape.gobarber.model.Address;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class BarberDTO {

    @Schema(description = "Nome do barbeiro", example = "Zé Tramontina")
    private String name;

    @Schema(description = "Cpf do barbeiro", example = "12345678910")
    private String cpf;

    @Schema(description = "Endereço do barbeiro", example = "Rua dos bobos")
    private Integer address;

    @Schema(description = "Salário do barbeiro", example = "100000")
    private double salary;

    @Schema(description = "Data de admissão do barbeiro", example = "2024-09-11")
    private LocalDate admissionDate;

    @Schema(description = "Carga horária do barbeiro", example = "44")
    private Integer workload;
}


