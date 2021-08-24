from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class puzulkxzwbrcpeqe(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)

	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(3115.43,-2136.46,1347.77),
				velocity=Vector3(1772.971,-1309.991,154.101),
				angular_velocity=Vector3(-1.35351,2.42901,3.9126098),
				rotation=Rotator(-0.7807003,1.1648667,-0.76900375))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(532.5,15.179999,17.01),
						velocity=Vector3(434.281,-504.781,0.241),
						angular_velocity=Vector3(1.0999999E-4,-2.1E-4,-2.39681),
						rotation=Rotator(-0.009491506,-0.8557695,9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0),
				1: CarState(
					physics=Physics(
						location=Vector3(292.71,-631.39996,17.05),
						velocity=Vector3(171.83101,-893.42096,0.27099997),
						angular_velocity=Vector3(0.0,7.1E-4,-9.1E-4),
						rotation=Rotator(-0.016682042,-1.3809662,-9.58738E-5)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
