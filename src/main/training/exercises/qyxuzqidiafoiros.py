from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class qyxuzqidiafoiros(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)
	
	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-2425.75,2584.74,1145.91),
				velocity=Vector3(512.511,704.40094,-604.78094),
				angular_velocity=Vector3(-3.14131,-4.37051,2.65151),
				rotation=Rotator(0.19069299,0.6780195,-0.7032343))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-3764.44,393.4,17.05),
						velocity=Vector3(72.081,1007.98096,0.341),
						angular_velocity=Vector3(0.0,0.0,-2.38321),
						rotation=Rotator(-0.016682042,1.4550767,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=100.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-3202.8198,1214.9099,17.01),
						velocity=Vector3(463.521,894.061,0.211),
						angular_velocity=Vector3(-1.0999999E-4,5.1E-4,6.1E-4),
						rotation=Rotator(-0.00958738,1.0926737,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
