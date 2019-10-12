from dataclasses import dataclass, field
from typing import Any

from rlbot.utils.game_state_util import *
from rlbottraining.common_graders.goal_grader import *
from rlbottraining.training_exercise import TrainingExercise

@dataclass
class udgubayyrzbfngjy(TrainingExercise):
	grader: Any = PassOnGoalForAllyTeam(ally_team=0)
	
	def make_game_state(self,rng):
		return GameState(
			ball=BallState(physics=Physics(
				location=Vector3(-3294.72,-1829.22,151.05),
				velocity=Vector3(435.781,1645.6609,7.0109997),
				angular_velocity=Vector3(0.29281,3.45171,-0.28391),
				rotation=Rotator(0.88098437,0.52558017,-1.2318825))),
			cars={
				0: CarState(
					physics=Physics(
						location=Vector3(-3027.3499,-752.42,17.05),
						velocity=Vector3(663.60095,1122.971,0.26099998),
						angular_velocity=Vector3(0.0,-2.1E-4,0.087309994),
						rotation=Rotator(-0.016682042,1.0388885,0.0)),
					jumped=False,
					double_jumped=False,
					boost_amount=85.0),
				1: CarState(
					physics=Physics(
						location=Vector3(-3428.27,-2116.67,91.579994),
						velocity=Vector3(495.181,1724.4209,257.651),
						angular_velocity=Vector3(-5.09291,2.07231,-0.12880999),
						rotation=Rotator(-0.53660566,1.169948,0.01361408)),
					jumped=True,
					double_jumped=True,
					boost_amount=0.0)
			},
			boosts={i: BoostState(0) for i in range(34)},
		)
