from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class kdbsiexyxxfsiuzr(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)
	
	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(756.35,-822.27997,240.0),
				velocity=Vector3(2705.8809,1133.7009,454.501),
				angular_velocity=Vector3(-5.23701,-2.81791,0.79521),
				rotation=Rotator(0.57725614,-0.10143448,-1.7429857))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(415.62,-823.63,94.39),
						velocity=Vector3(1971.261,1181.661,-14.321),
						angular_velocity=Vector3(-2.04811,5.10261,-0.13441),
						rotation=Rotator(-1.4940014,2.9179192,-2.5415184)),
					jumped=True,
					double_jumped=True,
					boost_amount=88.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-137.48,-1861.98,17.01),
						velocity=Vector3(289.251,1040.241,0.251),
						angular_velocity=Vector3(-1.0999999E-4,0.0,-2.3791099),
						rotation=Rotator(-0.009491506,1.2454965,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
