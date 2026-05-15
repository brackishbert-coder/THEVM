package workbench;

import java.util.List;

public record ValidationResultDTO(
		 boolean valid,
		 List<String> errors,
		 List<String> warnings
		) {}