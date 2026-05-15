package workbench;

public record CurvatureSampleDTO(
		 double x,
		 double y,
		 double scalarCurvature,
		 boolean isSingular,
		 boolean isFlat
		) {}